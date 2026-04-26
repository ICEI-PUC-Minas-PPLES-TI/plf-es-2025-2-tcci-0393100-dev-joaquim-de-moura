package br.com.seunome.mobulite.ui

fun onlyDigits(value: String): String {
    return value.filter { it.isDigit() }
}

fun formatPhoneBr(value: String): String {
    val digits = onlyDigits(value).take(11)

    return when {
        digits.length <= 2 -> digits
        digits.length <= 7 -> "(${digits.take(2)}) ${digits.drop(2)}"
        else -> "(${digits.take(2)}) ${digits.drop(2).take(5)}-${digits.drop(7)}"
    }
}