package com.example.controledegastos.ui.features.help

import androidx.appcompat.app.AppCompatActivity
import com.example.controledegastos.ui.configureSystemInsets
import android.os.Bundle
import com.example.controledegastos.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureSystemInsets()

        binding.backIconHelpActivity.setOnClickListener { finish() }

    }
}
