package com.zenithblue.fb2Pro.domain.model

import androidx.compose.runtime.Composable

sealed interface ItemValue {
    data class Text(val name: String, val value: String) : ItemValue
}

@Composable
fun ItemValue.getName(): String {
    return when (this) {
        is ItemValue.Text -> this.name
    }
}

@Composable
fun ItemValue.getValue(): String {
    return when (this) {
        is ItemValue.Text -> this.value
    }
}