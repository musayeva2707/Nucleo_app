package com.nucleo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.nucleo.content.Elements
import com.nucleo.content.HomeSystemIngredients
import com.nucleo.content.Missions
import com.nucleo.content.PlanetType
import com.nucleo.content.Remnant
import com.nucleo.content.StarType
import com.nucleo.content.StarTypeId
import com.nucleo.data.SaveStore
import com.nucleo.game.AchievementList
import com.nucleo.game.Cell
import com.nucleo.game.CellStatus
import com.nucleo.game.Economy
import com.nucleo.game.ForgeEngine
import com.nucleo.game.GalaxyEngine
import com.nucleo.game.GameState
import com.nucleo.game.MissionRecord
import com.nucleo.game.ModeStats
import com.nucleo.game.Mode
import com.nucleo.game.OrbitSim
import com.nucleo.game.Pool
import com.nucleo.game.Progression
import com.nucleo.game.PuzzleItem
import com.nucleo.game.ReactionEngine
import com.nucleo.game.Settings
import com.nucleo.game.SpectrumEngine
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** One guess row in Spectrum Lab. */
data class Guess(val item: PuzzleItem, val cells: List<Cell>)

/** A Spectrum Lab round in progress. Kept in memory only. */
data class SpectrumRound(
    val pool: Pool,
    val mode: Mode,
    val answerId: String,
    val maxGuesses: Int,
    val guesses: List<Guess> = emptyList(),
    /** Hard mode: column indexes hidden this round. */
    val hidden: Set<Int> = emptySet(),
    /** Column index → exact answer value, revealed by hints. */
    val hints: Map<Int, String> = emptyMap(),
    val outcome: RoundOutcome? = null,
    val blitzSecondsLeft: Int = 0,
    val blitzSolves: Int = 0,
) {
    val answer: PuzzleItem get() = SpectrumEngine.item(pool, answerId)
    val guessedIds: Set<String> get() = guesses.map { it.item.id }.toSet()
    /** Hard mode hides the spectrum strip until guess 3. */
    val showStrip: Boolean get() = mode != Mode.HARD || guesses.size >= 2
}

data class RoundOutcome(
    val solved: Boolean,
    val guessesUsed: Int,
    val rp: Int,
    val baseRp: Int,
    val modeBonus: Double,
    val streak: Int,
    /** Signal mode: the element that was just identified. */
    val unlockedZ: Int? = null,
    val blitzSolves: Int = 0,
)

/** What the Forge made while the app was closed. */
data class OfflineReport(val realSeconds: Long, val gameSeconds: Double, val gains: List<Pair<Int, Int>>)

data class MissionReward(val stars: Int, val fuelUsed: Int, val firstClear: Boolean, val rp: Int, val plateId: String?)

/**
 * The single source of truth for the UI. Screens read [state] and call the functions below.
 * All game rules live in the pure-Kotlin engines in `com.nucleo.game`; this class only wires them up,
 * keeps the clock running and saves.
 */
class GameViewModel(private val store: SaveStore) : ViewModel() {

    private val random = Random.Default

    private val _state = MutableStateFlow(store.loadGame() ?: newGame())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _round = MutableStateFlow<SpectrumRound?>(null)
    val round: StateFlow<SpectrumRound?> = _round.asStateFlow()

    private val _galaxyFeedback = MutableStateFlow<GalaxyEngine.Feedback?>(null)
    val galaxyFeedback: StateFlow<GalaxyEngine.Feedback?> = _galaxyFeedback.asStateFlow()

    private val _offlineReport = MutableStateFlow<OfflineReport?>(null)
    val offlineReport: StateFlow<OfflineReport?> = _offlineReport.asStateFlow()

    /** Short messages for the snackbar. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private var ticker: Job? = null
    private var reactionMisses = 0

    private fun newGame() = GameState(firstPlayedEpochMs = System.currentTimeMillis(), lastSeenEpochMs = System.currentTimeMillis())

    // ---- Lifecycle: the clock runs only while the app is visible ---------------------------

    fun onAppForeground() {
        applyOfflineProgress()
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            var last = System.nanoTime()
            var sinceSave = 0.0
            while (true) {
                delay(100)
                val now = System.nanoTime()
                val dt = (now - last) / 1e9
                last = now
                advance(dt)
                tickBlitz(dt)
                sinceSave += dt
                if (sinceSave >= 30) {
                    save()
                    sinceSave = 0.0
                }
            }
        }
    }

    fun onAppBackground() {
        ticker?.cancel()
        ticker = null
        save()
    }

    private fun save() {
        val s = _state.value.copy(lastSeenEpochMs = System.currentTimeMillis())
        _state.value = s
        store.saveGame(s)
    }

    private fun advance(dt: Double) {
        val tick = ForgeEngine.advance(_state.value, dt, random)
        commit(tick.state)
        announce(tick)
    }

    private fun announce(tick: ForgeEngine.Tick) {
        for (report in tick.reports) {
            if (!report.event.fullScreen) message("${report.event.label}! A white dwarf remains.")
        }
        for (z in tick.newSignals) message("New signal detected. Identify it in Spectrum Lab.")
    }

    /** Offline seconds count at reduced speed, up to the cap. Clock going backwards counts as 0. */
    private fun applyOfflineProgress() {
        val s = _state.value
        val now = System.currentTimeMillis()
        val elapsed = max(0L, now - s.lastSeenEpochMs) / 1000.0
        if (s.lastSeenEpochMs == 0L || elapsed < 10) return
        val effective = Progression.offlineSpeed(s) * min(elapsed, Progression.offlineCapSec(s))

        var next = s
        var left = effective
        while (left > 0) {
            val step = min(60.0, left)
            next = ForgeEngine.advance(next, step, random).state
            left -= step
        }
        val gains = next.elements.mapNotNull { (z, amount) ->
            val gained = (amount - s.amount(z)).toInt()
            if (gained > 0) z to gained else null
        }.sortedByDescending { it.second }
        commit(next.copy(lastSeenEpochMs = now))
        if (gains.isNotEmpty()) _offlineReport.value = OfflineReport(elapsed.toLong(), effective, gains)
    }

    fun dismissOfflineReport() {
        _offlineReport.value = null
    }

    /** Every state change goes through here: it records milestones and achievements. */
    private fun commit(newState: GameState) {
        var s = newState
        val now = System.currentTimeMillis()
        fun mark(id: String, reached: Boolean) {
            if (reached && id !in s.milestones) s = s.copy(milestones = s.milestones + (id to now))
        }
        mark("first_light", s.stats.starsFormed > 0)
        mark("first_supernova", s.stats.supernovae > 0)
        mark("first_molecule", s.discoveredRecipes.isNotEmpty())
        mark("first_flight", s.missionsCleared > 0)
        mark("first_ocean", s.endingSeen)

        for (a in AchievementList.newlyEarned(s)) {
            s = s.copy(achievements = s.achievements + (a.id to now))
            message("Achievement: ${a.name}")
        }
        _state.value = s
    }

    private fun message(text: String) {
        _messages.tryEmit(text)
    }

    /** Shows a short snackbar message (used by navigation for locked tabs). */
    fun showMessage(text: String) = message(text)

    fun finishIntro() = commit(_state.value.copy(introSeen = true))

    // ---- Star Forge ------------------------------------------------------------------------

    private var tapWindowStartMs = 0L
    private var tapsInWindow = 0

    /** Each tap adds 5 H. At most 10 taps per second are counted. */
    fun tapNebula() {
        val now = System.currentTimeMillis()
        if (now - tapWindowStartMs >= 1000) {
            tapWindowStartMs = now
            tapsInWindow = 0
        }
        if (++tapsInWindow > 10) return
        val tick = ForgeEngine.tapNebula(_state.value)
        commit(tick.state)
        announce(tick)
    }

    fun formStar(type: StarType, slot: Int): Boolean {
        val next = ForgeEngine.formStar(_state.value, type, slot) ?: return false
        commit(next)
        return true
    }

    fun triggerSupernova(starId: Int) = commit(ForgeEngine.triggerSupernova(_state.value, starId, random))

    fun merge(remnant: Remnant) {
        val next = ForgeEngine.startMerger(_state.value, remnant) ?: return
        commit(next)
        message(if (remnant == Remnant.NEUTRON_STAR) "Neutron stars spiralling in. Kilonova in 30 min." else "White dwarfs merging. Type Ia in 20 min.")
    }

    fun dismissEvent() {
        val s = _state.value
        if (s.eventQueue.isNotEmpty()) commit(s.copy(eventQueue = s.eventQueue.drop(1)))
    }

    fun activateStarburst() {
        val next = GalaxyEngine.activateStarburst(_state.value) ?: return
        commit(next)
        message("Starburst! ×3 hydrogen for 5 minutes.")
    }

    // ---- Spectrum Lab ----------------------------------------------------------------------

    fun startRound(pool: Pool, mode: Mode) {
        val s = _state.value
        val answerId: String
        if (mode == Mode.SIGNAL) {
            val z = s.signals.firstOrNull() ?: return
            answerId = Elements[z].symbol
        } else {
            val (id, save) = SpectrumEngine.nextAnswer(s.spectrum, pool, mode)
            answerId = id
            commit(s.copy(spectrum = save))
        }
        val maxGuesses = when (mode) {
            Mode.SIGNAL -> SpectrumEngine.SIGNAL_GUESSES
            Mode.CLASSIC -> Progression.classicGuesses(s)
            Mode.HARD -> 6
            Mode.BLITZ -> SpectrumEngine.BLITZ_GUESSES
        }
        val previous = _round.value
        _round.value = SpectrumRound(
            pool = if (mode == Mode.SIGNAL) Pool.ELEMENTS else pool,
            mode = mode,
            answerId = answerId,
            maxGuesses = maxGuesses,
            hidden = if (mode == Mode.HARD) SpectrumEngine.hiddenColumns(pool, random) else emptySet(),
            // A Blitz run keeps its clock and score from answer to answer.
            blitzSecondsLeft = if (mode == Mode.BLITZ) (previous?.takeIf { it.mode == Mode.BLITZ && it.outcome == null }?.blitzSecondsLeft
                ?: SpectrumEngine.BLITZ_START_SECONDS) else 0,
            blitzSolves = if (mode == Mode.BLITZ) (previous?.takeIf { it.mode == Mode.BLITZ && it.outcome == null }?.blitzSolves ?: 0) else 0,
        )
    }

    fun guess(itemId: String) {
        val r = _round.value ?: return
        if (r.outcome != null || itemId in r.guessedIds) return
        val cells = SpectrumEngine.score(r.pool, r.answerId, itemId)
        val updated = r.copy(guesses = r.guesses + Guess(SpectrumEngine.item(r.pool, itemId), cells))
        _round.value = updated

        val solved = itemId == r.answerId
        when {
            solved && r.mode == Mode.BLITZ -> blitzSolved(updated)
            solved -> finishRound(updated, solved = true)
            updated.guesses.size < updated.maxGuesses -> Unit
            r.mode == Mode.SIGNAL -> {
                // No fail state: reveal one column and add 2 guesses.
                val column = firstUnrevealedColumn(updated)
                _round.value = updated.copy(
                    maxGuesses = updated.maxGuesses + 2,
                    hints = if (column == null) updated.hints else updated.hints + (column to SpectrumEngine.answerValue(r.pool, r.answerId, column)),
                )
                message("Here's a clue, and 2 more guesses.")
            }
            r.mode == Mode.BLITZ -> {
                message("It was ${updated.answer.name}.")
                recordSpectrum(updated, solved = false)
                startRound(r.pool, Mode.BLITZ)
            }
            else -> finishRound(updated, solved = false)
        }
    }

    private fun blitzSolved(r: SpectrumRound) {
        recordSpectrum(r, solved = true)
        val s = _state.value
        commit(s.copy(rp = s.rp + SpectrumEngine.BLITZ_RP))
        message("+${SpectrumEngine.BLITZ_RP} RP · +${SpectrumEngine.BLITZ_BONUS_SECONDS} s")
        _round.value = r.copy(blitzSolves = r.blitzSolves + 1, blitzSecondsLeft = r.blitzSecondsLeft + SpectrumEngine.BLITZ_BONUS_SECONDS)
        startRound(r.pool, Mode.BLITZ)
    }

    private var blitzCarry = 0.0
    private fun tickBlitz(dt: Double) {
        val r = _round.value ?: return
        if (r.mode != Mode.BLITZ || r.outcome != null) return
        blitzCarry += dt
        if (blitzCarry < 1.0) return
        blitzCarry -= 1.0
        val left = r.blitzSecondsLeft - 1
        if (left > 0) {
            _round.value = r.copy(blitzSecondsLeft = left)
        } else {
            val s = _state.value
            val key = SpectrumEngine.key(r.pool, Mode.BLITZ)
            val stats = s.spectrum.stats[key] ?: ModeStats()
            commit(s.copy(spectrum = s.spectrum.copy(stats = s.spectrum.stats + (key to stats.copy(bestRun = max(stats.bestRun, r.blitzSolves))))))
            _round.value = r.copy(
                blitzSecondsLeft = 0,
                outcome = RoundOutcome(true, 0, r.blitzSolves * SpectrumEngine.BLITZ_RP, SpectrumEngine.BLITZ_RP, 1.0, 0, blitzSolves = r.blitzSolves),
            )
        }
    }

    private fun recordSpectrum(r: SpectrumRound, solved: Boolean) {
        val s = _state.value
        var save = SpectrumEngine.recordRound(s.spectrum, r.pool, r.mode, solved)
        save = SpectrumEngine.scheduleRepeat(save, r.pool, r.mode, r.answerId, solved, r.guesses.size)
        val solvedCount = s.stats.puzzlesSolved[r.pool.name] ?: 0
        commit(s.copy(
            spectrum = save,
            stats = if (solved) s.stats.copy(puzzlesSolved = s.stats.puzzlesSolved + (r.pool.name to solvedCount + 1)) else s.stats,
        ))
    }

    private fun finishRound(r: SpectrumRound, solved: Boolean) {
        val before = _state.value.spectrum.stats[SpectrumEngine.key(r.pool, r.mode)]?.streak ?: 0
        if (r.mode != Mode.SIGNAL) recordSpectrum(r, solved)
        val streak = if (solved) before + 1 else 0
        val rp = if (solved) SpectrumEngine.rpFor(r.mode, r.guesses.size, streak) else 0

        var s = _state.value.copy(rp = _state.value.rp + rp)
        var unlocked: Int? = null
        if (r.mode == Mode.SIGNAL && solved) {
            val z = Elements.bySymbol(r.answerId)!!.z
            unlocked = z
            s = s.copy(
                identified = s.identified + z,
                signals = s.signals - z,
                stats = s.stats.copy(puzzlesSolved = s.stats.puzzlesSolved + ("SIGNAL" to (s.stats.puzzlesSolved["SIGNAL"] ?: 0) + 1)),
            )
        }
        commit(s)
        _round.value = r.copy(
            outcome = RoundOutcome(
                solved = solved,
                guessesUsed = r.guesses.size,
                rp = rp,
                baseRp = if (r.mode == Mode.SIGNAL) SpectrumEngine.SIGNAL_RP else SpectrumEngine.BASE_RP[min(max(1, r.guesses.size), 7) - 1],
                modeBonus = if (r.mode == Mode.HARD) 1.5 else 1.0,
                streak = streak,
                unlockedZ = unlocked,
            )
        )
    }

    private fun firstUnrevealedColumn(r: SpectrumRound): Int? {
        val exactSoFar = r.guesses.flatMap { g -> g.cells.withIndex().filter { it.value.status == CellStatus.EXACT }.map { it.index } }.toSet()
        return SpectrumEngine.columns(r.pool).indices.firstOrNull { it !in r.hints && it !in exactSoFar && it !in r.hidden }
            ?: SpectrumEngine.columns(r.pool).indices.firstOrNull { it !in r.hints }
    }

    /** Hint: 1 token, or 20 RP when out of tokens. Reveals one column's exact value. */
    fun useHint() {
        val r = _round.value ?: return
        if (r.outcome != null) return
        val column = firstUnrevealedColumn(r) ?: return
        val s = _state.value
        val paid = when {
            s.hintTokens > 0 -> s.copy(hintTokens = s.hintTokens - 1)
            s.rp >= SpectrumEngine.HINT_RP_COST -> s.copy(rp = s.rp - SpectrumEngine.HINT_RP_COST)
            else -> { message("No hint tokens and not enough RP."); return }
        }
        commit(paid)
        _round.value = r.copy(hints = r.hints + (column to SpectrumEngine.answerValue(r.pool, r.answerId, column)))
    }

    fun giveUp() {
        val r = _round.value ?: return
        if (r.outcome != null || r.mode == Mode.SIGNAL) return
        finishRound(r, solved = false)
    }

    fun nextRound() {
        val r = _round.value ?: return
        if (r.mode == Mode.SIGNAL && _state.value.signals.isEmpty()) {
            _round.value = null
            return
        }
        _round.value = null
        startRound(r.pool, r.mode)
    }

    fun closeRound() {
        _round.value = null
    }

    // ---- Reaction Lab ----------------------------------------------------------------------

    fun react(ingredientIds: List<String>, batches: Int): ReactionEngine.Result {
        val result = ReactionEngine.react(_state.value, ingredientIds, batches)
        when (result) {
            is ReactionEngine.Result.Made -> {
                reactionMisses = 0
                commit(result.state)
            }
            ReactionEngine.Result.NoRecipe -> {
                reactionMisses++
                if (reactionMisses >= 3) {
                    val tip = ReactionEngine.suggestion(_state.value, ingredientIds)
                    if (tip != null) message("Try: ${tip.inputs.keys.joinToString(" + ") { ReactionEngine.moleculeName(it) }}")
                    reactionMisses = 0
                }
            }
            else -> Unit
        }
        return result
    }

    // ---- Gravity Slingshot -----------------------------------------------------------------

    fun buildPlanet(type: PlanetType) {
        val next = ReactionEngine.buildPlanet(_state.value, type) ?: return
        commit(next)
        message("${type.label} built. It stays in your hangar forever.")
    }

    /** Pays the launch fuel. Failure costs nothing but this fuel. */
    fun payLaunch(fuel: Int): Boolean {
        val s = _state.value
        if (s.fuel < fuel) return false
        commit(s.copy(fuel = s.fuel - fuel))
        return true
    }

    fun completeMission(missionId: String, fuelUsed: Int): MissionReward {
        val mission = Missions[missionId]
        val s = _state.value
        val old = s.missions[missionId] ?: MissionRecord()
        val stars = OrbitSim.starsFor(fuelUsed, mission.parFuel)
        val firstClear = old.bestStars == 0
        val record = MissionRecord(
            bestStars = max(old.bestStars, stars),
            bestFuel = if (old.bestFuel == 0) fuelUsed else min(old.bestFuel, fuelUsed),
        )
        commit(s.copy(
            missions = s.missions + (missionId to record),
            rp = s.rp + if (firstClear) mission.rewardRp else 0,
            plates = if (firstClear) s.plates + mission.plateId else s.plates,
        ))
        return MissionReward(stars, fuelUsed, firstClear, if (firstClear) mission.rewardRp else 0, if (firstClear) mission.plateId else null)
    }

    // ---- Galaxy Sorter ---------------------------------------------------------------------

    fun startNight() {
        val next = GalaxyEngine.startNight(_state.value, random) ?: return
        commit(next)
    }

    fun answerGalaxy(pickName: String) {
        val (next, feedback) = GalaxyEngine.answer(_state.value, pickName) ?: return
        commit(next)
        _galaxyFeedback.value = feedback
    }

    fun nextGalaxy() {
        _galaxyFeedback.value = null
        commit(GalaxyEngine.nextImage(_state.value))
    }

    fun finishNight() {
        _galaxyFeedback.value = null
        commit(_state.value.copy(night = null))
    }

    // ---- Upgrades --------------------------------------------------------------------------

    fun buyUpgrade(id: String) {
        val next = Economy.buy(_state.value, id) ?: return
        commit(next)
    }

    // ---- Home System and ending ------------------------------------------------------------

    /** Sun-like stars that are still on the main sequence can host life. */
    fun homeStarCandidates(s: GameState) = s.stars.filter { it.type == StarTypeId.SUN_LIKE }

    fun chooseHomeStar(starId: Int) = commit(_state.value.let { it.copy(home = it.home.copy(homeStarId = starId)) })

    fun setOrbit(value: Float) = commit(_state.value.let { it.copy(home = it.home.copy(orbit = value)) })

    fun placeHomePlanet() {
        val s = _state.value
        if (s.planetCount(PlanetType.ROCKY) == 0) {
            message("Build a rocky planet in the Hangar first.")
            return
        }
        commit(s.copy(home = s.home.copy(planetPlaced = true)))
    }

    fun deliver(moleculeId: String) {
        val s = _state.value
        val need = HomeSystemIngredients.first { it.first == moleculeId }.second
        val have = s.home.delivered[moleculeId] ?: 0
        val give = min(s.molecule(moleculeId), need - have)
        if (give <= 0) return
        commit(s.copy(
            molecules = s.molecules + (moleculeId to s.molecule(moleculeId) - give),
            home = s.home.copy(delivered = s.home.delivered + (moleculeId to have + give)),
        ))
    }

    fun beginEnding() = commit(_state.value.copy(endingSeen = true))

    // ---- Settings and save file ------------------------------------------------------------

    fun updateSettings(change: (Settings) -> Settings) {
        val next = change(_settings.value)
        _settings.value = next
        store.saveSettings(next)
    }

    fun exportSave(): String = store.encodeGame(_state.value.copy(lastSeenEpochMs = System.currentTimeMillis()))

    fun importSave(text: String): Boolean {
        val imported = store.decodeGame(text) ?: return false
        _round.value = null
        _state.value = imported
        save()
        message("Save imported.")
        return true
    }

    fun resetProgress() {
        store.deleteGame()
        _round.value = null
        _galaxyFeedback.value = null
        _state.value = newGame()
        save()
    }

    companion object {
        /** Creates the ViewModel with its SaveStore (manual dependency injection, no Hilt needed). */
        fun factory(store: SaveStore) = viewModelFactory {
            initializer { GameViewModel(store) }
        }
    }
}
