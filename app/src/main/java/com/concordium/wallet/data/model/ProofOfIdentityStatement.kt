package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ProofOfIdentityStatement(
    val type: AttributeType?,
    val attributeTag: AttributeTag?,
    val lower: String?,
    val upper: String?,
    val set: List<String>?
) : Serializable {

    fun revealAttributeType(): Boolean {
        return type == AttributeType.REVEAL_ATTRIBUTE
    }

    fun attributeInSetType(): Boolean {
        return type == AttributeType.ATTRIBUTE_IN_SET
    }

    fun attributeNotInSetType(): Boolean {
        return type == AttributeType.ATTRIBUTE_NOT_IN_SET
    }

    fun attributeInRangeType(): Boolean {
        return type == AttributeType.ATTRIBUTE_IN_RANGE
    }

    fun checkFirstName(): Boolean {
        return attributeTag == AttributeTag.FIRST_NAME
    }

    fun checkLastName(): Boolean {
        return attributeTag == AttributeTag.LAST_NAME
    }

    fun checkSex(): Boolean {
        return attributeTag == AttributeTag.SEX
    }

    fun checkDob(): Boolean {
        return attributeTag == AttributeTag.DOB
    }

    fun checkCountryOfResidence(): Boolean {
        return attributeTag == AttributeTag.COUNTRY_OF_RESIDENCE
    }

    fun checkNationality(): Boolean {
        return attributeTag == AttributeTag.NATIONALITY
    }

    fun checkIdDocumentType(): Boolean {
        return attributeTag == AttributeTag.ID_DOC_TYPE
    }

    fun checkIdDocumentNumber(): Boolean {
        return attributeTag == AttributeTag.ID_DOC_NUMBER
    }

    fun checkIdDocumentIssuer(): Boolean {
        return attributeTag == AttributeTag.ID_DOC_ISSUER
    }

    fun checkIdDocumentIssuedAt(): Boolean {
        return attributeTag == AttributeTag.ID_DOC_ISSUED_AT
    }

    fun checkIdDocumentExpiresAt(): Boolean {
        return attributeTag == AttributeTag.ID_DOC_EXPIRES_AT
    }

    fun checkNationalIdNumber(): Boolean {
        return attributeTag == AttributeTag.NATIONAL_ID_NUMBER
    }

    fun checkTaxIdNumber(): Boolean {
        return attributeTag == AttributeTag.TAX_ID_NUMBER
    }

    fun isInTimeRange(timeStr: String): Boolean {
        val date = LocalDate.parse(timeStr, DateTimeFormatter.ofPattern("YYYYMMDD"))
        var upperDate: LocalDate? = null
        var lowerDate: LocalDate? = null
        var timeValidated = true
        upper?.let {
            upperDate = LocalDate.parse(it, DateTimeFormatter.ofPattern("YYYYMMDD"))
        }
        lower?.let {
            lowerDate = LocalDate.parse(it, DateTimeFormatter.ofPattern("YYYYMMDD"))
        }

        if (upperDate != null) {
            if (date!!.isAfter(upperDate)) {
                timeValidated = false
            }
        }

        if (lowerDate != null) {
            if (date!!.isBefore(lowerDate)) {
                timeValidated = false
            }
        }
        return timeValidated
    }

    fun isInSet(data: String): Boolean {
        if (set == null) {
            return false
        }
        return set.contains(data)
    }

    fun notInSet(data: String): Boolean {
        if (set == null) {
            return false
        }
        return !set.contains(data)
    }

}

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