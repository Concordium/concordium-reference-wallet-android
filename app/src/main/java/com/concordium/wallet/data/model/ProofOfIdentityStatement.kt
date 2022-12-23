package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ProofOfIdentityStatement(
    val type: String?,
    val attributeTag: String?,
    val lower: String?,
    val upper: String?,
    val set: List<String>?
) : Serializable

enum class AttributeTag(val tag: String) {
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    SEX("sex"),
    DOB("dob"),
    COUNTRY_OF_RESIDENCE("countryOfResidence"),
    NATIONALITY("nationality"),
    ID_DOC_TYPE("idDocType"),
    ID_DOC_NUMBER("idDocNo"),
    ID_DOC_ISSUER("idDocIssuer"),
    ID_DOC_ISSUED_AT("idDocIssuedAt"),
    ID_DOC_EXPIRES_AT("idDocExpiresAt"),
    NATIONAL_ID_NUMBER("nationalIdNo"),
    TAX_ID_NUMBER("taxIdNo"),


    // This has been added to have a default value
    @SerializedName("unknown")
    UNKNOWN("")
}

enum class AttributeType(val type: String) {
    REVEAL_ATTRIBUTE("RevealAttribute"),
    ATTRIBUTE_IN_SET("AttributeInSet"),
    ATTRIBUTE_NOT_IN_SET("AttributeNotInSet"),
    ATTRIBUTE_IN_RANGE("AttributeInRange"),
}