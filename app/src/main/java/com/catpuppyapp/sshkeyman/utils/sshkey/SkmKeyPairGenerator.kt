package com.catpuppyapp.sshkeyman.utils.sshkey

import java.nio.charset.Charset

interface SkmKeyPairGenerator {
    companion object {
        const val defaultCharset = "UTF-8"
        val defaultCharsetObj = Charset.forName(defaultCharset)  // this instance can shareable in multi threads

//    val bouncyCastleProviderStr="BC"

        val ED25529 = "Ed25519"  // recommend，但太麻烦，算了，不支持了
        val ECDSAP256 = "ECDSA(P256)"
        val RSA2048 = "RSA(2048)"
        val RSA4096 = "RSA(4096)"

        val algoList = listOf(ED25529, ECDSAP256, RSA2048, RSA4096)

    }

    /**
     * @param passphrase if null or empty, should no encrypt, else will encrypt the private key
     * @param algorithm one of the `SkmKeyPairGenerator.algoList`
     * @param keyLen for ed25519 the keyLen is nonsense, always 256
     * @param comment the comment usually is email
     *
     */
    fun generateKeyPair(passphrase: String, algorithm:String, keyLen:Int, comment:String) :SkmKeyPair
}
