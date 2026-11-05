package uz.nucleo.game

import uz.nucleo.content.CosmicEvent
import uz.nucleo.content.Elements
import uz.nucleo.content.MergerTimers
import uz.nucleo.content.Remnant
import uz.nucleo.content.StarPhase
import uz.nucleo.content.StarType
import uz.nucleo.content.StarTypeId
import uz.nucleo.content.StarTypes
import uz.nucleo.content.Upgrades
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * The Star Forge simulation. Pure Kotlin.
 *
 * Star output is a closed-form function of time: for any time window we overlap the window with
 * each phase and multiply by that phase's rates. No per-second loop is needed, so offline
 * catch-up is instant.
 */
object ForgeEngine {

    /** The result of moving time forward or tapping: the new state, bursts that happened and new signals. */
    data class Tick(
        val state: GameState,
        val reports: List<EventReport> = emptyList(),
        val newSignals: List<Int> = emptyList(),
    )

    const val TAP_HYDROGEN = 5.0
    private const val H_PER_HELIUM = 12.0
    private const val H_PER_LITHIUM = 100_000.0

    // ---- Star helpers ----------------------------------------------------------------------

    fun typeOf(star: Star): StarType = StarTypes[star.type]

    /** Age in "1× seconds", i.e. how far through its life the star is. */
    fun age(star: Star, nowSec: Double): Double = max(0.0, (nowSec - star.formedAtSec) * star.speed)

    /** Index of the current phase, or phases.size when the star finished its last phase. */
    fun phaseIndex(star: Star, nowSec: Double): Int {
        var start = 0.0
        val a = age(star, nowSec)
        typeOf(star).phases.forEachIndexed { i, phase ->
            if (a < start + phase.seconds) return i
            start += phase.seconds
        }
        return typeOf(star).phases.size
    }

    /** 0..1 progress through the current phase. */
    fun phaseProgress(star: Star, nowSec: Double): Float {
        val phases = typeOf(star).phases
        val index = phaseIndex(star, nowSec)
        if (index >= phases.size) return 1f
        val phase = phases[index]
        if (phase.seconds.isInfinite()) return 0f
        val start = phases.take(index).sumOf { it.seconds }
        return ((age(star, nowSec) - start) / phase.seconds).toFloat().coerceIn(0f, 1f)
    }

    /** Game seconds left in the current phase. */
    fun secondsLeftInPhase(star: Star, nowSec: Double): Double {
        val phases = typeOf(star).phases
        val index = phaseIndex(star, nowSec)
        if (index >= phases.size) return 0.0
        val end = phases.take(index + 1).sumOf { it.seconds }
        return (end - age(star, nowSec)) / star.speed
    }

    /** A massive star that finished Si burning: its supernova can be triggered. */
    fun isReadyToExplode(star: Star, nowSec: Double): Boolean =
        star.type == StarTypeId.MASSIVE && phaseIndex(star, nowSec) >= typeOf(star).phases.size

    /** Seconds until a ready supernova fires on its own. */
    fun supernovaCountdown(star: Star, nowSec: Double): Double {
        val readyAt = star.formedAtSec + typeOf(star).lifetimeSec / star.speed
        return max(0.0, readyAt + StarTypes.SUPERNOVA_AUTO_SECONDS - nowSec)
    }

    /** Everything this star has made so far (main rates only, without random trace elements). */
    fun totalOutput(star: Star, nowSec: Double): Map<Int, Double> {
        val gains = HashMap<Int, Double>()
        val a = age(star, nowSec)
        var phaseStart = 0.0
        for (phase in typeOf(star).phases) {
            val overlap = min(a, phaseStart + phase.seconds) - phaseStart
            if (overlap > 0) for ((z, rate) in phase.ratesPerSec) gains[z] = (gains[z] ?: 0.0) + rate * overlap
            phaseStart += phase.seconds
        }
        return gains
    }

    fun currentRates(star: Star, nowSec: Double): Map<Int, Double> {
        val phase = typeOf(star).phases.getOrNull(phaseIndex(star, nowSec)) ?: return emptyMap()
        return phase.ratesPerSec.mapValues { it.value * star.speed }
    }

    // ---- Player actions --------------------------------------------------------------------

    fun tapNebula(state: GameState): Tick = gatherHydrogen(state, TAP_HYDROGEN)

    /** Forms a star in [slot]. Returns null if the player can't afford it or the slot is taken. */
    fun formStar(state: GameState, type: StarType, slot: Int): GameState? {
        val cost = Progression.starCost(state, type)
        if (state.amount(1) < cost) return null
        if (state.stars.any { it.slot == slot }) return null
        if (slot >= Progression.starSlots(state)) return null
        if (!Progression.canFormType(state, type)) return null

        val isTutorialStar = type.id == StarTypeId.MASSIVE && !state.firstMassiveDiscountUsed
        val star = Star(
            id = state.nextStarId,
            type = type.id,
            slot = slot,
            formedAtSec = state.gameTimeSec,
            speed = Progression.phaseSpeed(state) * if (isTutorialStar) StarTypes.TUTORIAL_SPEED else 1.0,
        )
        return state.copy(
            elements = state.elements + (1 to state.amount(1) - cost),
            stars = state.stars + star,
            nextStarId = state.nextStarId + 1,
            firstMassiveDiscountUsed = state.firstMassiveDiscountUsed || type.id == StarTypeId.MASSIVE,
            stats = state.stats.copy(starsFormed = state.stats.starsFormed + 1),
        )
    }

    /** Triggers the supernova of a ready massive star now (instead of waiting 10 s). */
    fun triggerSupernova(state: GameState, starId: Int, random: Random): GameState {
        val star = state.stars.firstOrNull { it.id == starId } ?: return state
        if (!isReadyToExplode(star, state.gameTimeSec)) return state
        return killStar(state, star, random).first
    }

    /** Starts merging two remnants. The burst happens when the timer ends. */
    fun startMerger(state: GameState, remnant: Remnant): GameState? = when (remnant) {
        Remnant.NEUTRON_STAR -> if (state.neutronStars < 2) null else state.copy(
            neutronStars = state.neutronStars - 2,
            mergers = state.mergers + Merger(CosmicEvent.KILONOVA, state.gameTimeSec + MergerTimers.KILONOVA_SECONDS),
        )
        Remnant.WHITE_DWARF -> if (state.whiteDwarfs < 2) null else state.copy(
            whiteDwarfs = state.whiteDwarfs - 2,
            mergers = state.mergers + Merger(CosmicEvent.TYPE_IA, state.gameTimeSec + MergerTimers.TYPE_IA_SECONDS),
        )
    }

    // ---- Time ------------------------------------------------------------------------------

    /**
     * Moves the Forge forward by [dt] game seconds. Returns the new state and any bursts that
     * happened (supernovae, nebulae, mergers), in time order.
     */
    fun advance(state: GameState, dt: Double, random: Random): Tick {
        if (dt <= 0) return Tick(state)
        val from = state.gameTimeSec
        val to = from + dt
        val reports = mutableListOf<EventReport>()
        val newSignals = mutableListOf<Int>()

        // 1. Nebula hydrogen (plus helium and lithium trickle)
        val nebula = gatherHydrogen(state, Progression.nebulaRate(state) * dt)
        var s = nebula.state
        newSignals += nebula.newSignals

        // 2. Star output over [from, to]
        val gains = HashMap<Int, Double>()
        for (star in s.stars) {
            addStarOutput(star, from, to, gains, random)
        }
        val (afterStars, starSignals) = addElements(s, gains)
        s = afterStars.copy(gameTimeSec = to)
        newSignals += starSignals

        // 3. Deaths: sun-like stars die on their own; massive stars explode 10 s after they're ready
        for (star in s.stars.toList()) {
            val type = typeOf(star)
            val endOfLife = star.formedAtSec + type.lifetimeSec / star.speed
            val dueAt = if (type.id == StarTypeId.MASSIVE) endOfLife + StarTypes.SUPERNOVA_AUTO_SECONDS else endOfLife
            if (type.deathEvent != null && to >= dueAt) {
                val (next, report) = killStar(s, star, random)
                s = next
                if (report != null) reports += report
            }
        }

        // 4. Remnant mergers that finished
        for (merger in s.mergers.filter { it.endsAtSec <= to }) {
            val (next, report) = burst(s.copy(mergers = s.mergers - merger), merger.event, merger.event.label, null, random)
            s = next.withStat(merger.event).copy(eventQueue = next.eventQueue + report)
            reports += report
        }

        // 5. Cosmic-ray hits (upgrade)
        if (s.hasUpgrade(Upgrades.COSMIC_RAY_DETECTOR) && to >= s.nextCosmicRayAtSec) {
            val pick = listOf(3, 4, 5).random(random)
            val (next, raySignals) = addElements(s, mapOf(pick to 1.0))
            s = next.copy(nextCosmicRayAtSec = to + MergerTimers.COSMIC_RAY_EVERY_SECONDS)
            newSignals += raySignals
        }

        return Tick(s, reports, newSignals + reports.flatMap { it.newSignals })
    }

    // ---- Internals -------------------------------------------------------------------------

    private fun gatherHydrogen(state: GameState, hydrogen: Double): Tick {
        val heliumCarry = state.heliumCarry + hydrogen
        val lithiumCarry = state.lithiumCarry + hydrogen
        val helium = floor(heliumCarry / H_PER_HELIUM)
        val lithium = floor(lithiumCarry / H_PER_LITHIUM)

        val gains = buildMap {
            put(1, hydrogen)
            if (helium > 0) put(2, helium)
            if (lithium > 0) put(3, lithium)
        }
        val (next, newSignals) = addElements(state, gains)
        val result = next.copy(
            heliumCarry = heliumCarry - helium * H_PER_HELIUM,
            lithiumCarry = lithiumCarry - lithium * H_PER_LITHIUM,
            stats = next.stats.copy(hydrogenGathered = next.stats.hydrogenGathered + hydrogen),
        )
        return Tick(result, newSignals = newSignals)
    }

    private fun addStarOutput(star: Star, from: Double, to: Double, gains: MutableMap<Int, Double>, random: Random) {
        val a0 = age(star, from)
        val a1 = age(star, to)
        var phaseStart = 0.0
        for (phase in typeOf(star).phases) {
            val phaseEnd = phaseStart + phase.seconds
            val overlap = min(a1, phaseEnd) - max(a0, phaseStart)
            if (overlap > 0) {
                for ((z, rate) in phase.ratesPerSec) {
                    gains[z] = (gains[z] ?: 0.0) + rate * overlap
                }
                addTraceElements(phase, phaseStart, a0, a1, gains, random)
            }
            phaseStart = phaseEnd
        }
    }

    /** One random s-process element per minute of AGB phase. */
    private fun addTraceElements(phase: StarPhase, phaseStart: Double, a0: Double, a1: Double, gains: MutableMap<Int, Double>, random: Random) {
        if (phase.tracePool.isEmpty()) return
        val minutesBefore = floor(max(0.0, a0 - phaseStart) / 60.0).toInt()
        val minutesAfter = floor(min(phase.seconds, a1 - phaseStart) / 60.0).toInt()
        repeat(max(0, minutesAfter - minutesBefore)) {
            val z = phase.tracePool.random(random)
            gains[z] = (gains[z] ?: 0.0) + 1.0
        }
    }

    /** Removes a dead star and applies its death burst. Full-screen events are queued for the overlay. */
    private fun killStar(state: GameState, star: Star, random: Random): Pair<GameState, EventReport?> {
        val type = typeOf(star)
        val event = type.deathEvent ?: return state to null
        val title = "Star ${star.slot + 1} · " + if (event == CosmicEvent.SUPERNOVA) "Core collapse" else "End of life"
        val (afterBurst, report) = burst(state.copy(stars = state.stars - star), event, title, type.remnant, random)
        val withRemnant = when (type.remnant) {
            Remnant.NEUTRON_STAR -> afterBurst.copy(neutronStars = afterBurst.neutronStars + 1)
            Remnant.WHITE_DWARF -> afterBurst.copy(whiteDwarfs = afterBurst.whiteDwarfs + 1)
            null -> afterBurst
        }
        val withStat = withRemnant.withStat(event)
        val queued = if (event.fullScreen) withStat.copy(eventQueue = withStat.eventQueue + report) else withStat
        return queued to report
    }

    /** Adds an event's fixed yield plus 1–3 random trace elements from its origin pool. */
    private fun burst(state: GameState, event: CosmicEvent, title: String, remnant: Remnant?, random: Random): Pair<GameState, EventReport> {
        val yields = event.yields.toMutableMap()
        val pool = Elements.withOrigin(event.traceOrigin).filter { !it.isHumanMade }.map { it.z }
        if (pool.isNotEmpty()) {
            repeat(random.nextInt(1, 4)) {
                val z = pool.random(random)
                yields[z] = (yields[z] ?: 0) + random.nextInt(1, 4)
            }
        }
        val (next, newSignals) = addElements(state, yields.mapValues { it.value.toDouble() })
        val report = EventReport(event, title, yields.toList().sortedByDescending { it.second }.toMap(), newSignals, remnant)
        return next to report
    }

    private fun GameState.withStat(event: CosmicEvent): GameState = copy(
        stats = when (event) {
            CosmicEvent.SUPERNOVA -> stats.copy(supernovae = stats.supernovae + 1)
            CosmicEvent.KILONOVA -> stats.copy(kilonovae = stats.kilonovae + 1)
            CosmicEvent.TYPE_IA -> stats.copy(typeIa = stats.typeIa + 1)
            else -> stats
        }
    )

    /**
     * Adds element amounts. Any element produced for the first time that isn't identified yet
     * joins the signal queue. Returns the new state and the new signals.
     */
    fun addElements(state: GameState, gains: Map<Int, Double>): Pair<GameState, List<Int>> {
        if (gains.isEmpty()) return state to emptyList()
        val elements = state.elements.toMutableMap()
        val produced = state.produced.toMutableMap()
        val newSignals = mutableListOf<Int>()
        for ((z, amount) in gains) {
            if (amount <= 0) continue
            val firstTime = (produced[z] ?: 0.0) <= 0.0
            elements[z] = (elements[z] ?: 0.0) + amount
            produced[z] = (produced[z] ?: 0.0) + amount
            if (firstTime && z !in state.identified && z !in state.signals) newSignals += z
        }
        return state.copy(
            elements = elements,
            produced = produced,
            signals = state.signals + newSignals,
        ) to newSignals
    }
}
