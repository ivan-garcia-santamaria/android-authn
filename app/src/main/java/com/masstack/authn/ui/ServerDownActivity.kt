package com.masstack.authn.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masstack.authn.R
import com.masstack.authn.data.repositories.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.math.abs
import kotlin.random.Random

@AndroidEntryPoint
class ServerDownActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorMessage = intent.getStringExtra("error_message") ?: "Service Unavailable"
        val errorCode = intent.getIntExtra("error_code", 503)

        setContent {
            MaterialTheme {
                val settings = settingsRepository.getSettings()
                ServerDownScreen(
                    errorMessage = errorMessage,
                    errorCode = errorCode,
                    selectedGame = settings.serverDownGame,
                    onBack = { finish() },
                    onRetry = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDownScreen(
    errorMessage: String,
    errorCode: Int,
    selectedGame: String = "SNAKE",
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Unavailable") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row: Goku Image + Error Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goku Image (left side)
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.goku),
                    contentDescription = "Goku",
                    modifier = Modifier
                        .width(120.dp)
                        .padding(end = 16.dp),
                    contentScale = ContentScale.FillWidth
                )

                // Error Card (center, takes remaining space)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (errorCode > 0) "HTTP $errorCode" else "Server Unavailable",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Game (based on selection)
            when (selectedGame) {
                "TETRIS" -> TetrisGame()
                "SPACE_INVADERS" -> SpaceInvadersGame()
                else -> SnakeGame() // Default to Snake
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Retry Button
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Game Selection
enum class GameType { SNAKE, TETRIS, SPACE_INVADERS }

// Snake Game Implementation
data class Position(val x: Int, val y: Int)

enum class Direction { UP, DOWN, LEFT, RIGHT }

@Composable
fun SnakeGame() {
    val gridSize = 15
    var snake by remember { mutableStateOf(listOf(Position(7, 7))) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var food by remember { mutableStateOf(Position(Random.nextInt(gridSize), Random.nextInt(gridSize))) }
    var score by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    // Game loop
    LaunchedEffect(isGameOver, isPaused, hasStarted) {
        while (!isGameOver && !isPaused && hasStarted) {
            delay(200)

            val head = snake.first()
            val newHead = when (direction) {
                Direction.UP -> Position(head.x, (head.y - 1 + gridSize) % gridSize)
                Direction.DOWN -> Position(head.x, (head.y + 1) % gridSize)
                Direction.LEFT -> Position((head.x - 1 + gridSize) % gridSize, head.y)
                Direction.RIGHT -> Position((head.x + 1) % gridSize, head.y)
            }

            // Check collision with self
            if (snake.contains(newHead)) {
                isGameOver = true
                continue
            }

            val newSnake = listOf(newHead) + snake
            snake = if (newHead == food) {
                // Eat food
                score++
                food = Position(Random.nextInt(gridSize), Random.nextInt(gridSize))
                newSnake
            } else {
                newSnake.dropLast(1)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🐍 SNAKE GAME 🐍",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Puntuación: $score",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Game Grid with swipe controls
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .border(2.dp, MaterialTheme.colorScheme.outline)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (!isGameOver) {
                                val (dx, dy) = dragAmount
                                direction = when {
                                    abs(dx) > abs(dy) -> if (dx > 0) Direction.RIGHT else Direction.LEFT
                                    else -> if (dy > 0) Direction.DOWN else Direction.UP
                                }
                            }
                        }
                    }
            ) {
                // Draw grid
                Column(modifier = Modifier.fillMaxSize()) {
                    for (y in 0 until gridSize) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (x in 0 until gridSize) {
                                val position = Position(x, y)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            when {
                                                snake.first() == position -> Color(0xFF4CAF50) // Head
                                                snake.contains(position) -> Color(0xFF8BC34A) // Body
                                                food == position -> Color(0xFFFF5722) // Food
                                                else -> Color.Black
                                            }
                                        )
                                        .padding(1.dp)
                                )
                            }
                        }
                    }
                }

                // Game Over overlay
                if (isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GAME OVER",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "Puntuación: $score",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            if (!isGameOver) {
                Text(
                    text = "Desliza en cualquier dirección para mover",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!hasStarted && !isGameOver) {
                    // Start button (shown initially)
                    Button(
                        onClick = { hasStarted = true },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("▶️ START")
                    }
                } else {
                    if (!isGameOver) {
                        Button(onClick = { isPaused = !isPaused }) {
                            Text(if (isPaused) "▶️ Continuar" else "⏸️ Pausa")
                        }
                    }

                    Button(
                        onClick = {
                            snake = listOf(Position(7, 7))
                            direction = Direction.RIGHT
                            food = Position(Random.nextInt(gridSize), Random.nextInt(gridSize))
                            score = 0
                            isGameOver = false
                            isPaused = false
                            hasStarted = false
                        }
                    ) {
                        Text("🔄 Nuevo Juego")
                    }
                }
            }
        }
    }
}

// Tetris Game Implementation
data class TetrisPosition(val x: Int, val y: Int)

enum class TetrisShape { I, O, T, S, Z, L, J }

data class TetrisPiece(
    val shape: TetrisShape,
    val blocks: List<TetrisPosition>,
    val color: Color
) {
    fun moveDown(): TetrisPiece = copy(blocks = blocks.map { it.copy(y = it.y + 1) })
    fun moveLeft(): TetrisPiece = copy(blocks = blocks.map { it.copy(x = it.x - 1) })
    fun moveRight(): TetrisPiece = copy(blocks = blocks.map { it.copy(x = it.x + 1) })

    fun rotate(): TetrisPiece {
        if (shape == TetrisShape.O) return this // O piece doesn't rotate

        val pivot = blocks[1] // Second block is the pivot
        val rotated = blocks.map { block ->
            val relX = block.x - pivot.x
            val relY = block.y - pivot.y
            TetrisPosition(pivot.x - relY, pivot.y + relX)
        }
        return copy(blocks = rotated)
    }

    companion object {
        fun random(): TetrisPiece {
            val shape = TetrisShape.entries.random()
            val blocks = when (shape) {
                TetrisShape.I -> listOf(
                    TetrisPosition(3, 0), TetrisPosition(4, 0),
                    TetrisPosition(5, 0), TetrisPosition(6, 0)
                )
                TetrisShape.O -> listOf(
                    TetrisPosition(4, 0), TetrisPosition(5, 0),
                    TetrisPosition(4, 1), TetrisPosition(5, 1)
                )
                TetrisShape.T -> listOf(
                    TetrisPosition(4, 0), TetrisPosition(3, 1),
                    TetrisPosition(4, 1), TetrisPosition(5, 1)
                )
                TetrisShape.S -> listOf(
                    TetrisPosition(4, 0), TetrisPosition(5, 0),
                    TetrisPosition(3, 1), TetrisPosition(4, 1)
                )
                TetrisShape.Z -> listOf(
                    TetrisPosition(3, 0), TetrisPosition(4, 0),
                    TetrisPosition(4, 1), TetrisPosition(5, 1)
                )
                TetrisShape.L -> listOf(
                    TetrisPosition(5, 0), TetrisPosition(3, 1),
                    TetrisPosition(4, 1), TetrisPosition(5, 1)
                )
                TetrisShape.J -> listOf(
                    TetrisPosition(3, 0), TetrisPosition(3, 1),
                    TetrisPosition(4, 1), TetrisPosition(5, 1)
                )
            }
            val color = when (shape) {
                TetrisShape.I -> Color(0xFF00F0F0) // Cyan
                TetrisShape.O -> Color(0xFFF0F000) // Yellow
                TetrisShape.T -> Color(0xFFA000F0) // Purple
                TetrisShape.S -> Color(0xFF00F000) // Green
                TetrisShape.Z -> Color(0xFFF00000) // Red
                TetrisShape.L -> Color(0xFFF0A000) // Orange
                TetrisShape.J -> Color(0xFF0000F0) // Blue
            }
            return TetrisPiece(shape, blocks, color)
        }
    }
}

@Composable
fun TetrisGame() {
    val gridWidth = 10
    val gridHeight = 16

    var currentPiece by remember { mutableStateOf<TetrisPiece?>(null) }
    var placedBlocks by remember { mutableStateOf<Map<TetrisPosition, Color>>(emptyMap()) }
    var score by remember { mutableStateOf(0) }
    var lines by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    var gameSpeed by remember { mutableStateOf(500L) }

    fun canMoveTo(piece: TetrisPiece): Boolean {
        return piece.blocks.all { block ->
            block.x in 0 until gridWidth &&
            block.y < gridHeight &&
            !placedBlocks.containsKey(block)
        }
    }

    fun placePiece(piece: TetrisPiece) {
        val newPlaced = placedBlocks.toMutableMap()
        piece.blocks.forEach { block ->
            newPlaced[block] = piece.color
        }
        placedBlocks = newPlaced

        // Check for completed lines
        val completedLines = (0 until gridHeight).filter { y ->
            (0 until gridWidth).all { x -> placedBlocks.containsKey(TetrisPosition(x, y)) }
        }

        if (completedLines.isNotEmpty()) {
            // Remove completed lines
            val filtered = placedBlocks.filter { !completedLines.contains(it.key.y) }

            // Move down blocks above completed lines
            val moved = filtered.mapKeys { (pos, _) ->
                val linesBelow = completedLines.count { it > pos.y }
                pos.copy(y = pos.y + linesBelow)
            }

            placedBlocks = moved
            lines += completedLines.size
            score += when (completedLines.size) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> 0
            }

            // Increase speed every 10 lines
            gameSpeed = (500 - (lines / 10) * 50).coerceAtLeast(100).toLong()
        }
    }

    // Game loop
    LaunchedEffect(isGameOver, isPaused, hasStarted, gameSpeed) {
        while (!isGameOver && !isPaused && hasStarted) {
            delay(gameSpeed)

            if (currentPiece == null) {
                val newPiece = TetrisPiece.random()
                if (canMoveTo(newPiece)) {
                    currentPiece = newPiece
                } else {
                    isGameOver = true
                    continue
                }
            }

            currentPiece?.let { piece ->
                val movedPiece = piece.moveDown()
                if (canMoveTo(movedPiece)) {
                    currentPiece = movedPiece
                } else {
                    placePiece(piece)
                    currentPiece = null
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🧩 TETRIS 🧩",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Puntos: $score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Líneas: $lines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Game Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.625f)
                    .border(2.dp, MaterialTheme.colorScheme.outline)
                    .background(Color.Black)
            ) {
                // Draw grid
                Column(modifier = Modifier.fillMaxSize()) {
                    for (y in 0 until gridHeight) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (x in 0 until gridWidth) {
                                val pos = TetrisPosition(x, y)
                                val blockColor = when {
                                    currentPiece?.blocks?.contains(pos) == true -> currentPiece!!.color
                                    placedBlocks.containsKey(pos) -> placedBlocks[pos]!!
                                    else -> Color.Black
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(blockColor)
                                        .padding(0.5.dp)
                                )
                            }
                        }
                    }
                }

                // Game Over overlay
                if (isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GAME OVER",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "Puntos: $score",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Líneas: $lines",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control buttons
            if (!hasStarted && !isGameOver) {
                Button(
                    onClick = { hasStarted = true },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("▶️ START")
                }
            } else {
                // Game controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!isGameOver) {
                        Button(onClick = { isPaused = !isPaused }) {
                            Text(if (isPaused) "▶️" else "⏸️")
                        }
                    }

                    Button(
                        onClick = {
                            currentPiece = null
                            placedBlocks = emptyMap()
                            score = 0
                            lines = 0
                            isGameOver = false
                            isPaused = false
                            hasStarted = false
                            gameSpeed = 500L
                        }
                    ) {
                        Text("🔄")
                    }
                }

                if (!isGameOver && hasStarted) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Movement controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                currentPiece?.let { piece ->
                                    val rotated = piece.rotate()
                                    if (canMoveTo(rotated)) {
                                        currentPiece = rotated
                                    }
                                }
                            }
                        ) {
                            Text("↻")
                        }
                        Button(
                            onClick = {
                                currentPiece?.let { piece ->
                                    val moved = piece.moveLeft()
                                    if (canMoveTo(moved)) {
                                        currentPiece = moved
                                    }
                                }
                            }
                        ) {
                            Text("←")
                        }
                        Button(
                            onClick = {
                                currentPiece?.let { piece ->
                                    val moved = piece.moveDown()
                                    if (canMoveTo(moved)) {
                                        currentPiece = moved
                                        score += 1
                                    }
                                }
                            }
                        ) {
                            Text("↓")
                        }
                        Button(
                            onClick = {
                                currentPiece?.let { piece ->
                                    val moved = piece.moveRight()
                                    if (canMoveTo(moved)) {
                                        currentPiece = moved
                                    }
                                }
                            }
                        ) {
                            Text("→")
                        }
                    }
                }
            }
        }
    }
}

// Space Invaders Game Implementation
data class Alien(val x: Float, val y: Float)
data class Bullet(val x: Float, val y: Float, val isPlayerBullet: Boolean)

@Composable
fun SpaceInvadersGame() {
    val gridWidth = 10f
    val gridHeight = 15f

    var playerX by remember { mutableStateOf(gridWidth / 2f) }
    var aliens by remember { mutableStateOf<List<Alien>>(emptyList()) }
    var bullets by remember { mutableStateOf<List<Bullet>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    var alienDirection by remember { mutableStateOf(1f) }

    // Initialize aliens
    LaunchedEffect(hasStarted) {
        if (hasStarted && aliens.isEmpty()) {
            aliens = buildList {
                for (row in 0..2) {
                    for (col in 0..7) {
                        add(Alien(col.toFloat() + 1f, row.toFloat() + 1f))
                    }
                }
            }
        }
    }

    // Game loop
    LaunchedEffect(isGameOver, isPaused, hasStarted) {
        while (!isGameOver && !isPaused && hasStarted) {
            delay(150)

            // Move bullets
            bullets = bullets.mapNotNull { bullet ->
                val newY = if (bullet.isPlayerBullet) bullet.y - 0.5f else bullet.y + 0.5f
                if (newY < 0 || newY > gridHeight) null else bullet.copy(y = newY)
            }

            // Check bullet-alien collisions
            val hitAliens = mutableSetOf<Alien>()
            val hitBullets = mutableSetOf<Bullet>()

            bullets.filter { it.isPlayerBullet }.forEach { bullet ->
                aliens.forEach { alien ->
                    if (abs(bullet.x - alien.x) < 0.5f && abs(bullet.y - alien.y) < 0.5f) {
                        hitAliens.add(alien)
                        hitBullets.add(bullet)
                        score += 10
                    }
                }
            }

            aliens = aliens.filterNot { it in hitAliens }
            bullets = bullets.filterNot { it in hitBullets }

            // Move aliens
            if (aliens.isNotEmpty()) {
                val shouldMoveDown = aliens.any {
                    (it.x + alienDirection < 0.5f) || (it.x + alienDirection > gridWidth - 0.5f)
                }

                if (shouldMoveDown) {
                    alienDirection = -alienDirection
                    aliens = aliens.map { it.copy(y = it.y + 0.3f) }
                } else {
                    aliens = aliens.map { it.copy(x = it.x + alienDirection * 0.2f) }
                }

                // Alien shooting (random)
                if (Random.nextFloat() < 0.05f && aliens.isNotEmpty()) {
                    val shooter = aliens.random()
                    bullets = bullets + Bullet(shooter.x, shooter.y + 0.5f, false)
                }

                // Check if aliens reached player
                if (aliens.any { it.y > gridHeight - 2f }) {
                    isGameOver = true
                }
            }

            // Check if player hit by alien bullet
            bullets.filter { !it.isPlayerBullet }.forEach { bullet ->
                if (abs(bullet.x - playerX) < 0.5f && abs(bullet.y - (gridHeight - 1f)) < 0.5f) {
                    isGameOver = true
                }
            }

            // Check if all aliens destroyed
            if (aliens.isEmpty() && hasStarted) {
                isGameOver = true
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👾 SPACE INVADERS 👾",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Puntuación: $score",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Game Grid
            Box(
                modifier = Modifier
                    .aspectRatio(0.67f)
                    .border(2.dp, MaterialTheme.colorScheme.outline)
                    .background(Color.Black)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellWidth = size.width / gridWidth
                    val cellHeight = size.height / gridHeight

                    // Draw aliens
                    aliens.forEach { alien ->
                        drawCircle(
                            color = Color(0xFF00FF00),
                            radius = cellWidth * 0.4f,
                            center = Offset(alien.x * cellWidth, alien.y * cellHeight)
                        )
                    }

                    // Draw bullets
                    bullets.forEach { bullet ->
                        drawCircle(
                            color = if (bullet.isPlayerBullet) Color.Yellow else Color.Red,
                            radius = cellWidth * 0.15f,
                            center = Offset(bullet.x * cellWidth, bullet.y * cellHeight)
                        )
                    }

                    // Draw player
                    drawRect(
                        color = Color.Cyan,
                        topLeft = Offset(
                            (playerX - 0.4f) * cellWidth,
                            (gridHeight - 1.2f) * cellHeight
                        ),
                        size = Size(cellWidth * 0.8f, cellHeight * 0.4f)
                    )
                }

                // Game Over overlay
                if (isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (aliens.isEmpty() && score > 0) "¡VICTORIA!" else "GAME OVER",
                                color = if (aliens.isEmpty() && score > 0) Color(0xFF00FF00) else Color.White,
                                fontSize = 24.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "Puntuación: $score",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control buttons
            if (!hasStarted && !isGameOver) {
                Button(
                    onClick = { hasStarted = true },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("▶️ START")
                }
            } else {
                // Game controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!isGameOver) {
                        Button(onClick = { isPaused = !isPaused }) {
                            Text(if (isPaused) "▶️" else "⏸️")
                        }
                    }

                    Button(
                        onClick = {
                            playerX = gridWidth / 2f
                            aliens = buildList {
                                for (row in 0..2) {
                                    for (col in 0..7) {
                                        add(Alien(col.toFloat() + 1f, row.toFloat() + 1f))
                                    }
                                }
                            }
                            bullets = emptyList()
                            score = 0
                            isGameOver = false
                            isPaused = false
                            hasStarted = false
                            alienDirection = 1f
                        }
                    ) {
                        Text("🔄")
                    }
                }

                if (!isGameOver && hasStarted) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Movement and shooting controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (playerX > 0.5f) playerX -= 0.5f
                            }
                        ) {
                            Text("←")
                        }
                        Button(
                            onClick = {
                                bullets = bullets + Bullet(playerX, gridHeight - 2f, true)
                            }
                        ) {
                            Text("🔫 SHOOT")
                        }
                        Button(
                            onClick = {
                                if (playerX < gridWidth - 0.5f) playerX += 0.5f
                            }
                        ) {
                            Text("→")
                        }
                    }
                }
            }
        }
    }
}
