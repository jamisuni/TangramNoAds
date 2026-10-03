package io.github.jamisuni.tangram.devtools

// decision DA-78: The passcode is one standalone string constant.
internal object DevPasscode {
    const val CODE = "0417"

    fun accepts(input: String): Boolean = input == CODE
}
