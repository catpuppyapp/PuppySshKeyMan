package com.catpuppyapp.sshkeyman.utils.sshkey

import com.sshtools.common.publickey.SshKeyPairGenerator
import com.sshtools.common.publickey.SshPrivateKeyFileFactory
import com.sshtools.common.publickey.SshPublicKeyFileFactory
import java.nio.charset.StandardCharsets


class SshtoolsSkmKeyPairGen:SkmKeyPairGenerator {
    private fun getKeyTypeByAlgorithm(algorithm:String):String {
        return when (algorithm) {
            SkmKeyPairGenerator.ED25529 -> SshKeyPairGenerator.ED25519
            SkmKeyPairGenerator.ECDSAP256 -> SshKeyPairGenerator.ECDSA
            SkmKeyPairGenerator.RSA2048 -> SshKeyPairGenerator.SSH2_RSA
            SkmKeyPairGenerator.RSA4096 -> SshKeyPairGenerator.SSH2_RSA
            else -> throw IllegalArgumentException("doesn't support algorithm: $algorithm")
        }
    }

    override fun generateKeyPair(passphrase: String, algorithm: String, keyLen: Int, comment: String):SkmKeyPair {
//        val privateKeyFile: File = FsUtils.createTempKeyFile("privKey")
//        val publicKeyFile: File = FsUtils.createTempKeyFile("pubKey")

        val keyType = getKeyTypeByAlgorithm(algorithm)

        val pair = SshKeyPairGenerator.generateKeyPair(keyType, keyLen)
//        SshKeyUtils.createPublicKeyFile(pair.publicKey, comment, publicKeyFile)
//        SshKeyUtils.createPrivateKeyFile(pair, passphrase, privateKeyFile)
        val publicKeyFile = SshPublicKeyFileFactory.create(pair.publicKey, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT)
        val privateKeyFile = SshPrivateKeyFileFactory.create(pair, passphrase, SshPrivateKeyFileFactory.OPENSSH_FORMAT)

        val publicKey = String(publicKeyFile.formattedKey, StandardCharsets.UTF_8)
        val privateKey = String(privateKeyFile.formattedKey, StandardCharsets.UTF_8)

//        privateKeyFile.delete()
//        publicKeyFile.delete()

        return SkmKeyPair(
            privateKey = privateKey,
            publicKey = publicKey,
        )
    }
}
