package com.infinum.arkive.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.infinum.arkive.annotations.ArkiveView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

@ArkiveView(name = "Main Activity")
fun previewMainActivity(): Int = R.layout.activity_main
