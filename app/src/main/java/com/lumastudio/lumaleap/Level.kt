package com.lumastudio.lumaleap

import android.graphics.RectF

data class Platform(val rect: RectF)

data class Collectible(var x: Float, var y: Float, var collected: Boolean = false) {
    companion object {
        const val SIZE = 36f
    }
    fun bounds(): RectF = RectF(x, y, x + SIZE, y + SIZE)
}

class Level(
    val worldWidth: Float,
    val worldHeight: Float,
    val startX: Float,
    val startY: Float,
    val platforms: List<Platform>,
    val collectibles: MutableList<Collectible>,
    val enemyFactories: List<() -> Enemy>,
    val goal: RectF,
    val pitY: Float,       // if the player falls below this Y, they lose a life
    val themeSeed: Int     // used to pick a color palette for background variety
) {
    fun freshEnemies(): MutableList<Enemy> = enemyFactories.map { it() }.toMutableList()
}

object Levels {

    const val WORLD_HEIGHT = 1080f
    const val GROUND_Y = 900f

    /** Level 1: Glowing Meadow - gentle introduction */
    fun level1(): Level {
        val worldWidth = 4200f
        val platforms = mutableListOf(
            Platform(RectF(0f, GROUND_Y, 1100f, WORLD_HEIGHT)),
            Platform(RectF(1300f, GROUND_Y, 2100f, WORLD_HEIGHT)),
            Platform(RectF(1500f, GROUND_Y - 220f, 1750f, GROUND_Y - 190f)),
            Platform(RectF(2350f, GROUND_Y, 3200f, WORLD_HEIGHT)),
            Platform(RectF(2600f, GROUND_Y - 260f, 2850f, GROUND_Y - 230f)),
            Platform(RectF(3450f, GROUND_Y, worldWidth, WORLD_HEIGHT))
        )
        val collectibles = mutableListOf(
            Collectible(500f, GROUND_Y - 120f),
            Collectible(850f, GROUND_Y - 120f),
            Collectible(1600f, GROUND_Y - 300f),
            Collectible(2700f, GROUND_Y - 340f),
            Collectible(3600f, GROUND_Y - 120f)
        )
        val enemyFactories = listOf<() -> Enemy>(
            { Enemy(1500f, GROUND_Y - Enemy.HEIGHT, 1350f, 2000f, 200f) },
            { Enemy(2500f, GROUND_Y - Enemy.HEIGHT, 2400f, 3100f, 220f) }
        )
        val goal = RectF(worldWidth - 160f, GROUND_Y - 180f, worldWidth - 80f, GROUND_Y)
        return Level(
            worldWidth, WORLD_HEIGHT,
            startX = 80f, startY = GROUND_Y - Player.HEIGHT,
            platforms = platforms, collectibles = collectibles,
            enemyFactories = enemyFactories, goal = goal,
            pitY = WORLD_HEIGHT + 400f, themeSeed = 1
        )
    }

    /** Level 2: Crystal Hollow - more gaps and enemies */
    fun level2(): Level {
        val worldWidth = 5200f
        val platforms = mutableListOf(
            Platform(RectF(0f, GROUND_Y, 900f, WORLD_HEIGHT)),
            Platform(RectF(1150f, GROUND_Y, 1750f, WORLD_HEIGHT)),
            Platform(RectF(1450f, GROUND_Y - 230f, 1680f, GROUND_Y - 200f)),
            Platform(RectF(2000f, GROUND_Y, 2450f, WORLD_HEIGHT)),
            Platform(RectF(2750f, GROUND_Y, 3150f, WORLD_HEIGHT)),
            Platform(RectF(2900f, GROUND_Y - 300f, 3100f, GROUND_Y - 270f)),
            Platform(RectF(3450f, GROUND_Y, 3900f, WORLD_HEIGHT)),
            Platform(RectF(4200f, GROUND_Y, worldWidth, WORLD_HEIGHT))
        )
        val collectibles = mutableListOf(
            Collectible(400f, GROUND_Y - 120f),
            Collectible(1550f, GROUND_Y - 310f),
            Collectible(2150f, GROUND_Y - 120f),
            Collectible(2950f, GROUND_Y - 380f),
            Collectible(3650f, GROUND_Y - 120f),
            Collectible(4500f, GROUND_Y - 120f)
        )
        val enemyFactories = listOf<() -> Enemy>(
            { Enemy(1200f, GROUND_Y - Enemy.HEIGHT, 1150f, 1700f, 230f) },
            { Enemy(2050f, GROUND_Y - Enemy.HEIGHT, 2000f, 2400f, 250f) },
            { Enemy(2800f, GROUND_Y - Enemy.HEIGHT, 2750f, 3100f, 260f) },
            { Enemy(3500f, GROUND_Y - Enemy.HEIGHT, 3450f, 3850f, 260f) }
        )
        val goal = RectF(worldWidth - 160f, GROUND_Y - 180f, worldWidth - 80f, GROUND_Y)
        return Level(
            worldWidth, WORLD_HEIGHT,
            startX = 80f, startY = GROUND_Y - Player.HEIGHT,
            platforms = platforms, collectibles = collectibles,
            enemyFactories = enemyFactories, goal = goal,
            pitY = WORLD_HEIGHT + 400f, themeSeed = 2
        )
    }

    /** Level 3: Starlit Peak - hardest, narrow platforms and fast enemies */
    fun level3(): Level {
        val worldWidth = 6000f
        val platforms = mutableListOf(
            Platform(RectF(0f, GROUND_Y, 750f, WORLD_HEIGHT)),
            Platform(RectF(1000f, GROUND_Y, 1400f, WORLD_HEIGHT)),
            Platform(RectF(1650f, GROUND_Y - 200f, 1950f, GROUND_Y - 170f)),
            Platform(RectF(2100f, GROUND_Y, 2500f, WORLD_HEIGHT)),
            Platform(RectF(2750f, GROUND_Y - 320f, 3000f, GROUND_Y - 290f)),
            Platform(RectF(3150f, GROUND_Y, 3550f, WORLD_HEIGHT)),
            Platform(RectF(3800f, GROUND_Y, 4150f, WORLD_HEIGHT)),
            Platform(RectF(4300f, GROUND_Y - 240f, 4550f, GROUND_Y - 210f)),
            Platform(RectF(4700f, GROUND_Y, 5100f, WORLD_HEIGHT)),
            Platform(RectF(5350f, GROUND_Y, worldWidth, WORLD_HEIGHT))
        )
        val collectibles = mutableListOf(
            Collectible(300f, GROUND_Y - 120f),
            Collectible(1750f, GROUND_Y - 280f),
            Collectible(2250f, GROUND_Y - 120f),
            Collectible(2830f, GROUND_Y - 400f),
            Collectible(3300f, GROUND_Y - 120f),
            Collectible(3950f, GROUND_Y - 120f),
            Collectible(4400f, GROUND_Y - 320f),
            Collectible(4900f, GROUND_Y - 120f)
        )
        val enemyFactories = listOf<() -> Enemy>(
            { Enemy(1050f, GROUND_Y - Enemy.HEIGHT, 1000f, 1350f, 270f) },
            { Enemy(2150f, GROUND_Y - Enemy.HEIGHT, 2100f, 2450f, 280f) },
            { Enemy(3200f, GROUND_Y - Enemy.HEIGHT, 3150f, 3500f, 300f) },
            { Enemy(3850f, GROUND_Y - Enemy.HEIGHT, 3800f, 4100f, 300f) },
            { Enemy(4750f, GROUND_Y - Enemy.HEIGHT, 4700f, 5050f, 320f) }
        )
        val goal = RectF(worldWidth - 160f, GROUND_Y - 180f, worldWidth - 80f, GROUND_Y)
        return Level(
            worldWidth, WORLD_HEIGHT,
            startX = 80f, startY = GROUND_Y - Player.HEIGHT,
            platforms = platforms, collectibles = collectibles,
            enemyFactories = enemyFactories, goal = goal,
            pitY = WORLD_HEIGHT + 400f, themeSeed = 3
        )
    }

    fun forLevelNumber(n: Int): Level = when (n) {
        1 -> level1()
        2 -> level2()
        else -> level3()
    }
}
