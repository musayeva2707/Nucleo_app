package com.nucleo.ui.missions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import com.nucleo.AppNavigator
import com.nucleo.GameViewModel
import com.nucleo.MissionReward
import com.nucleo.content.Mission
import com.nucleo.content.Missions
import com.nucleo.content.OrbitRing
import com.nucleo.content.PlanetType
import com.nucleo.content.Point
import com.nucleo.game.FlightStatus
import com.nucleo.game.OrbitSim
import com.nucleo.game.PlacedPlanet
import com.nucleo.game.Probe
import com.nucleo.game.Progression
import com.nucleo.ui.components.FilterChip
import com.nucleo.ui.components.PrimaryButton
import com.nucleo.ui.components.QuietButton
import com.nucleo.ui.components.drawPlanet
import com.nucleo.ui.theme.LocalReduceMotion
import com.nucleo.ui.theme.NucleoColors
import com.nucleo.ui.theme.NucleoType

private enum class Phase(val label: String) { SETUP("Setup"), AIM("Aim"), FLIGHT("Flight"), RESULT("Result") }

/** Maps the 1000 × 1600 world onto the canvas, keeping its aspect ratio. */
private class WorldTransform(width: Float, height: Float) {
    val scale = min(width / Missions.WORLD_W.toFloat(), height / Missions.WORLD_H.toFloat())
    private val ox = (width - Missions.WORLD_W.toFloat() * scale) / 2
    private val oy = (height - Missions.WORLD_H.toFloat() * scale) / 2
    fun toScreen(x: Double, y: Double) = Offset(ox + x.toFloat() * scale, oy + y.toFloat() * scale)
    fun toScreen(p: Point) = toScreen(p.x, p.y)
    fun toWorld(o: Offset) = Point(((o.x - ox) / scale).toDouble(), ((o.y - oy) / scale).toDouble())
}

/**
 * 4.13 Mission play. Setup: place owned planets on orbit rings. Aim: drag back from the probe to set
 * direction and speed (or use the ±1° buttons). Flight: play/pause, 2× speed, free reset.
 */
@Composable
fun MissionPlayScreen(vm: GameViewModel, nav: AppNavigator, missionId: String) {
    val state by vm.state.collectAsStateWithLifecycle()
    val mission = remember(missionId) { Missions[missionId] }

    var phase by remember { mutableStateOf(Phase.SETUP) }
    val placements = remember { mutableStateListOf<PlacedPlanet>() }
    var selectedType by remember { mutableStateOf(PlanetType.entries.firstOrNull { state.planetCount(it) > 0 }) }
    var angle by remember { mutableDoubleStateOf(angleTo(mission.probe, mission.target.position)) }
    var speed by remember { mutableDoubleStateOf(200.0) }

    var probe by remember { mutableStateOf<Probe?>(null) }
    var simTime by remember { mutableDoubleStateOf(0.0) }
    val trail = remember { mutableStateListOf<Point>() }
    var paused by remember { mutableStateOf(false) }
    var fast by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf(FlightStatus.FLYING) }
    var launchFuel by remember { mutableIntStateOf(0) }
    var reward by remember { mutableStateOf<MissionReward?>(null) }

    val sim = remember(placements.toList()) { OrbitSim(mission, placements.toList()) }
    val fuelCost = OrbitSim.fuelCost(speed)
    val preview = if (phase == Phase.AIM) remember(sim, angle, speed) { sim.preview(angle, speed, Progression.previewSeconds(state)) } else emptyList()

    // Flight loop: fixed physics steps of 1/120 s, independent of frame rate.
    LaunchedEffect(phase, paused, fast) {
        if (phase != Phase.FLIGHT || paused) return@LaunchedEffect
        var last = withFrameNanos { it }
        var pending = 0.0
        while (true) {
            val now = withFrameNanos { it }
            pending += (now - last) / 1e9 * (if (fast) 2 else 1)
            last = now
            var p = probe ?: break
            var t = simTime
            var status = FlightStatus.FLYING
            while (pending >= OrbitSim.DT && status == FlightStatus.FLYING) {
                p = sim.step(p, t)
                t += OrbitSim.DT
                pending -= OrbitSim.DT
                status = sim.status(p, t)
            }
            probe = p
            simTime = t
            trail += Point(p.x, p.y)
            if (status != FlightStatus.FLYING) {
                outcome = status
                if (status == FlightStatus.WON) reward = vm.completeMission(mission.id, launchFuel)
                phase = Phase.RESULT
                break
            }
        }
    }

    fun launch() {
        if (!vm.payLaunch(fuelCost)) return
        launchFuel = fuelCost
        probe = sim.launch(angle, speed)
        simTime = 0.0
        trail.clear()
        paused = false
        phase = Phase.FLIGHT
    }

    fun resetToAim() {
        probe = null
        simTime = 0.0
        trail.clear()
        reward = null
        phase = Phase.AIM
    }

    Column(Modifier.fillMaxSize().background(NucleoColors.SpaceDeep)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Leave mission") }
            Column(Modifier.weight(1f)) {
                Text("${mission.id} · ${mission.name}", style = NucleoType.cardTitle)
                Text("Par ${mission.parFuel} fuel · reach the ring", style = NucleoType.caption, color = NucleoColors.TextSecondary)
            }
            Icon(Icons.Rounded.LocalGasStation, null, tint = NucleoColors.Missions, modifier = Modifier.size(16.dp))
            Text(" ${state.fuel}", style = NucleoType.numberMedium)
        }
        PhaseStepper(phase)

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val transform = WorldTransform(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            val reduceMotion = LocalReduceMotion.current
            val pulse by rememberInfiniteTransition(label = "target").animateFloat(
                0f, if (reduceMotion) 0f else 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "pulse",
            )
            Canvas(
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Mission canvas. Central star, orbit rings, the probe at the launch pad and the target ring" }
                    .pointerInput(phase) {
                        detectTapGestures { offset ->
                            if (phase == Phase.SETUP) {
                                selectedType?.let { type -> placeOrMove(mission, placements, type, state.planetCount(type), transform.toWorld(offset)) }
                            }
                        }
                    }
                    .pointerInput(phase) {
                        detectDragGestures { change, _ ->
                            val world = transform.toWorld(change.position)
                            when (phase) {
                                Phase.SETUP -> movePlanetAlongRing(mission, placements, world)
                                Phase.AIM -> {
                                    // Drag back from the probe, like a slingshot.
                                    val dx = mission.probe.x - world.x
                                    val dy = mission.probe.y - world.y
                                    val length = hypot(dx, dy)
                                    if (length > 10) {
                                        angle = atan2(dy, dx) * 180 / PI
                                        speed = (length * 1.2).coerceIn(OrbitSim.MIN_SPEED, OrbitSim.MAX_SPEED)
                                    }
                                }
                                else -> Unit
                            }
                        }
                    },
            ) {
                drawMission(mission, transform, sim, if (phase == Phase.FLIGHT || phase == Phase.RESULT) simTime else 0.0, pulse)
                if (phase == Phase.AIM) drawAim(mission, transform, angle, speed, preview)
                if (trail.isNotEmpty()) drawTrail(trail, transform)
                probe?.let { drawCircle(Color.White, 4 * density, transform.toScreen(it.x, it.y)) }
            }
            if (phase == Phase.AIM) {
                Text(
                    "Speed ${speed.toInt()} · Fuel $fuelCost",
                    style = NucleoType.numberSmall, color = NucleoColors.TextPrimary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        .background(NucleoColors.Surface.copy(alpha = 0.92f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Column(Modifier.fillMaxWidth().background(NucleoColors.Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (phase) {
                Phase.SETUP -> SetupControls(
                    mission, state.planets, placements.size, selectedType,
                    onSelect = { selectedType = it },
                    onClear = { placements.clear() },
                    onNext = { phase = Phase.AIM },
                )
                Phase.AIM -> AimControls(
                    fuelCost = fuelCost, fuel = state.fuel,
                    onRotate = { angle += it },
                    onSpeed = { speed = (speed + it).coerceIn(OrbitSim.MIN_SPEED, OrbitSim.MAX_SPEED) },
                    onBack = { phase = Phase.SETUP },
                    onLaunch = ::launch,
                )
                Phase.FLIGHT -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuietButton(if (paused) "Play" else "Pause", onClick = { paused = !paused }, icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, modifier = Modifier.weight(1f))
                    QuietButton(if (fast) "1×" else "2×", onClick = { fast = !fast }, icon = Icons.Rounded.FastForward, modifier = Modifier.weight(1f))
                    QuietButton("Reset", onClick = ::resetToAim, icon = Icons.Rounded.Replay, modifier = Modifier.weight(1f))
                }
                Phase.RESULT -> if (outcome == FlightStatus.WON) {
                    Text("Target reached!", style = NucleoType.title, color = NucleoColors.Missions)
                } else {
                    Text(
                        when (outcome) {
                            FlightStatus.CRASHED -> "Probe crashed. Try another angle."
                            FlightStatus.TIMEOUT -> "Probe drifted for 60 s. Try another angle."
                            else -> "Probe lost. Try another angle."
                        },
                        style = NucleoType.cardTitle,
                    )
                    Text("Failure costs nothing but the launch fuel.", style = NucleoType.caption, color = NucleoColors.TextSecondary)
                    PrimaryButton("Retry", onClick = ::resetToAim, color = NucleoColors.Missions, icon = Icons.Rounded.Replay, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    reward?.let { r ->
        MissionResultOverlay(
            mission = mission,
            reward = r,
            onNext = nav::back,
            onReplay = ::resetToAim,
            onTelescope = nav::openTelescope,
        )
    }
}

private fun angleTo(from: Point, to: Point) = atan2(to.y - from.y, to.x - from.x) * 180 / PI

/** The orbit ring closest to a world point, if the point is within 60 units of it. */
private fun ringNear(mission: Mission, world: Point): OrbitRing? {
    val distance = hypot(world.x - mission.center.x, world.y - mission.center.y)
    return mission.orbits.minByOrNull { abs(distance - it.radius) }?.takeIf { abs(distance - it.radius) < 60 }
}

private fun angleOnRing(mission: Mission, world: Point) = atan2(world.y - mission.center.y, world.x - mission.center.x) * 180 / PI

/** Tap a ring to place the selected planet there, or move the planet already on it. */
private fun placeOrMove(mission: Mission, placements: MutableList<PlacedPlanet>, type: PlanetType, owned: Int, world: Point) {
    val ring = ringNear(mission, world) ?: return
    val deg = angleOnRing(mission, world)
    val existing = placements.indexOfFirst { it.ringId == ring.id }
    when {
        existing >= 0 -> placements[existing] = placements[existing].copy(angleDeg = deg)
        type !in ring.allowed -> Unit
        placements.size >= mission.maxPlanets -> Unit
        placements.count { it.type == type } >= owned -> Unit
        else -> placements += PlacedPlanet(type, ring.id, deg)
    }
}

/** Drag a placed planet along its ring to set its starting angle. */
private fun movePlanetAlongRing(mission: Mission, placements: MutableList<PlacedPlanet>, world: Point) {
    val ring = ringNear(mission, world) ?: return
    val index = placements.indexOfFirst { it.ringId == ring.id }
    if (index >= 0) placements[index] = placements[index].copy(angleDeg = angleOnRing(mission, world))
}

@Composable
private fun PhaseStepper(phase: Phase) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Phase.SETUP, Phase.AIM, Phase.FLIGHT).forEach { p ->
            val done = p.ordinal < phase.ordinal
            val current = p == phase
            Row(
                Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (current) NucleoColors.Missions else NucleoColors.Surface2)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) Icon(Icons.Rounded.Check, null, tint = NucleoColors.Missions, modifier = Modifier.size(14.dp))
                Text(p.label, style = NucleoType.caption, color = if (current) NucleoColors.OnAccent else NucleoColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun SetupControls(
    mission: Mission,
    owned: Map<PlanetType, Int>,
    placedCount: Int,
    selected: PlanetType?,
    onSelect: (PlanetType) -> Unit,
    onClear: () -> Unit,
    onNext: () -> Unit,
) {
    Text("Planets $placedCount/${mission.maxPlanets} · pick one, then tap a dotted ring. Drag to move it.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        owned.filterValues { it > 0 }.forEach { (type, count) ->
            FilterChip("${type.label} ×$count", selected == type, { onSelect(type) }, accent = NucleoColors.Missions)
        }
    }
    val ringsText = mission.orbits.joinToString(" · ") { ring -> "ring ${ring.id.drop(1)}: " + ring.allowed.joinToString("/") { it.label.substringBefore(" ").lowercase() } }
    Text(ringsText, style = NucleoType.tiny, color = NucleoColors.TextSecondary)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuietButton("Clear", onClick = onClear, modifier = Modifier.weight(1f))
        PrimaryButton("Next: aim", onClick = onNext, color = NucleoColors.Missions, modifier = Modifier.weight(2f), height = 48.dp)
    }
}

@Composable
private fun AimControls(fuelCost: Int, fuel: Int, onRotate: (Double) -> Unit, onSpeed: (Double) -> Unit, onBack: () -> Unit, onLaunch: () -> Unit) {
    Text("Drag back from the probe to aim. Longer drag = faster and more fuel.", style = NucleoType.labelSmall, color = NucleoColors.TextSecondary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuietButton("−1°", onClick = { onRotate(-1.0) }, modifier = Modifier.weight(1f).semantics { contentDescription = "Rotate aim 1 degree counter-clockwise" })
        QuietButton("+1°", onClick = { onRotate(1.0) }, modifier = Modifier.weight(1f).semantics { contentDescription = "Rotate aim 1 degree clockwise" })
        QuietButton("−10", onClick = { onSpeed(-10.0) }, modifier = Modifier.weight(1f).semantics { contentDescription = "Slower" })
        QuietButton("+10", onClick = { onSpeed(10.0) }, modifier = Modifier.weight(1f).semantics { contentDescription = "Faster" })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuietButton("Setup", onClick = onBack, modifier = Modifier.weight(1f))
        PrimaryButton(
            if (fuel >= fuelCost) "Launch · $fuelCost fuel" else "Need $fuelCost fuel",
            onClick = onLaunch, enabled = fuel >= fuelCost, color = NucleoColors.Missions,
            icon = Icons.Rounded.RocketLaunch, modifier = Modifier.weight(2f), height = 48.dp,
        )
    }
    if (fuel < fuelCost) Text("Make hydrolox (2 H₂ + O₂ → 10 fuel) in Reaction Lab.", style = NucleoType.caption, color = NucleoColors.Warning)
}

// ---- Drawing -------------------------------------------------------------------------------

private val BeltColor = Color(0xFF8A7563)

private fun DrawScope.drawMission(mission: Mission, tf: WorldTransform, sim: OrbitSim, t: Double, pulse: Float) {
    // Faint grid
    var gx = 0.0
    while (gx <= Missions.WORLD_W) {
        var gy = 0.0
        while (gy <= Missions.WORLD_H) {
            drawCircle(NucleoColors.TextPrimary.copy(alpha = 0.08f), 1f * density, tf.toScreen(gx, gy))
            gy += 100.0
        }
        gx += 100.0
    }
    val c = tf.toScreen(mission.center)
    // Orbit rings
    mission.orbits.forEach { ring ->
        drawCircle(
            NucleoColors.TextPrimary.copy(alpha = 0.28f), (ring.radius * tf.scale).toFloat(), c,
            style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 8f * density))),
        )
    }
    // Asteroid belts (hazards)
    mission.belts.forEach { belt ->
        val r = (belt.radius * tf.scale).toFloat()
        drawArc(
            BeltColor.copy(alpha = 0.9f), startAngle = belt.fromDeg.toFloat(), sweepAngle = (belt.toDeg - belt.fromDeg).toFloat(), useCenter = false,
            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
            style = Stroke((belt.width * tf.scale).toFloat(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * density, 5f * density))),
        )
    }
    // Stars
    mission.stars.forEach { star ->
        val p = tf.toScreen(star.position)
        val r = (star.radius * tf.scale).toFloat()
        drawCircle(Brush.radialGradient(listOf(NucleoColors.SunLike, Color(0xFFFFE3B0).copy(alpha = 0.8f), NucleoColors.Forge.copy(alpha = 0f)), p, r * 3.4f), r * 3.4f, p)
        drawCircle(NucleoColors.SunLike, r, p)
    }
    // Target ring
    val target = tf.toScreen(mission.target.position)
    val tr = (mission.target.radius * tf.scale).toFloat()
    drawCircle(Brush.radialGradient(listOf(NucleoColors.Missions.copy(alpha = 0.35f), Color.Transparent), target, tr * 2.2f), tr * 2.2f, target)
    drawCircle(NucleoColors.Missions, tr, target, style = Stroke(2.5f * density))
    drawCircle(NucleoColors.Missions.copy(alpha = 0.9f * (1 - pulse)), tr * (1 + pulse * 0.9f), target, style = Stroke(1.5f * density))
    drawCircle(NucleoColors.Missions, 3f * density, target)
    // Planets
    for (i in 0 until sim.planetCount) {
        val p = sim.planetPosition(i, t)
        drawPlanet(sim.planetType(i), tf.toScreen(p), (sim.planetRadius(i) * tf.scale).toFloat())
    }
    // Launch pad
    val pad = tf.toScreen(mission.probe)
    drawRect(Color(0xFF2A334F), Offset(pad.x - 17 * density, pad.y + 6 * density), Size(34 * density, 8 * density))
    drawCircle(NucleoColors.TextPrimary, 5 * density, pad)
}

private fun DrawScope.drawAim(mission: Mission, tf: WorldTransform, angle: Double, speed: Double, preview: List<Point>) {
    val pad = tf.toScreen(mission.probe)
    val rad = angle * PI / 180
    // Drag-back handle on the opposite side of the launch direction.
    val back = (speed / 2.0)
    val handle = tf.toScreen(mission.probe.x - back * cos(rad), mission.probe.y - back * sin(rad))
    drawLine(NucleoColors.Missions, pad, handle, strokeWidth = 2 * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * density, 4 * density)), cap = StrokeCap.Round)
    drawCircle(NucleoColors.Missions.copy(alpha = 0.3f), 9 * density, handle)
    drawCircle(NucleoColors.Missions, 9 * density, handle, style = Stroke(2 * density))
    // Dotted preview of the first seconds of the path.
    preview.forEachIndexed { i, p ->
        val fade = 1f - i / preview.size.toFloat() * 0.7f
        drawCircle(NucleoColors.TextPrimary.copy(alpha = fade), 2f * density * fade, tf.toScreen(p))
    }
}

private fun DrawScope.drawTrail(trail: List<Point>, tf: WorldTransform) {
    for (i in 1 until trail.size) {
        val alpha = i / trail.size.toFloat()
        drawLine(NucleoColors.Missions.copy(alpha = alpha * 0.8f), tf.toScreen(trail[i - 1]), tf.toScreen(trail[i]), strokeWidth = 2 * density)
    }
}
