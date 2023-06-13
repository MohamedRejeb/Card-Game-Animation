package com.mohamedrejeb.cardsgame

import androidx.annotation.DrawableRes

data class Card(
    val id: Int,
    @DrawableRes val imageRes: Int,
)