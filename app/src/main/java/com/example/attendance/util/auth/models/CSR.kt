package com.example.attendance.util.auth.models

import kotlinx.serialization.Serializable


@Serializable
data class CSR(val publicKey: String, val name: String, val id: String, val token: String)

@Serializable
data class Certificate(val publicKey: String, val name: String, val id: String)

@Serializable
data class SignedCertificate(val certificate: Certificate, val signature: String)

@Serializable
data class SignedCertificateWithToken(val certificate: SignedCertificate, val token: String)
