package com.example.flappybird

import android.os.Bundle
import android.app.Activity


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GameView(this))

    }
}
