package com.concordium.wallet.util

import android.content.Context
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.*
import java.util.*

object AttributeRevealUtil {

    fun revealProof(
        statement: ProofOfIdentityStatement, data: HashMap<String, String>
    ): ProofReveal {
        val attributeTag: AttributeTag?
        val name: String?
        val value: String?
        var rawValue: String?
        val status: Boolean?

        val mContext = App.appContext

        when (statement.attributeTag) {

            AttributeTag.FIRST_NAME.tag -> {
                attributeTag = AttributeTag.FIRST_NAME
                name = mContext.getString(R.string.proof_of_identity_first_name)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = rawValue
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.LAST_NAME.tag -> {
                attributeTag = AttributeTag.LAST_NAME
                name = mContext.getString(R.string.proof_of_identity_last_name)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = rawValue
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.SEX.tag -> {
                attributeTag = AttributeTag.SEX
                name = mContext.getString(R.string.proof_of_identity_sex)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    val sex = convertSex(mContext, rawValue)
                    status = sex != null
                    if (status == false) {
                        rawValue = null
                        value = mContext.getString(R.string.proof_of_identity_not_available)
                    } else {
                        value = sex
                    }

                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.DOB.tag -> {
                attributeTag = AttributeTag.DOB
                name = mContext.getString(R.string.proof_of_identity_date_of_birth)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = DateTimeUtil.convertLongDate(rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.COUNTRY_OF_RESIDENCE.tag -> {
                attributeTag = AttributeTag.COUNTRY_OF_RESIDENCE
                name = mContext.getString(R.string.proof_of_identity_country_of_residence)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = getCountryName(rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.NATIONALITY.tag -> {
                attributeTag = AttributeTag.NATIONALITY
                name = mContext.getString(R.string.proof_of_identity_nationality)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = getCountryName(rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_TYPE.tag -> {
                attributeTag = AttributeTag.ID_DOC_TYPE
                name = mContext.getString(R.string.proof_of_identity_id_document_type)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = getDocType(mContext, rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_NUMBER.tag -> {
                attributeTag = AttributeTag.ID_DOC_NUMBER
                name = mContext.getString(R.string.proof_of_identity_id_document_number)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = rawValue
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_ISSUER.tag -> {
                attributeTag = AttributeTag.ID_DOC_ISSUER
                name = mContext.getString(R.string.proof_of_identity_id_document_issuer)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = getCountryName(rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_ISSUED_AT.tag -> {
                attributeTag = AttributeTag.ID_DOC_ISSUED_AT
                name = mContext.getString(R.string.proof_of_identity_id_document_issued_at)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = DateTimeUtil.convertLongDate(rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_EXPIRES_AT.tag -> {
                attributeTag = AttributeTag.ID_DOC_EXPIRES_AT
                name = mContext.getString(R.string.proof_of_identity_id_document_expires_at)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = DateTimeUtil.convertLongDate(rawValue)
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.NATIONAL_ID_NUMBER.tag -> {
                attributeTag = AttributeTag.NATIONAL_ID_NUMBER
                name = mContext.getString(R.string.proof_of_identity_national_id_number)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = rawValue
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.TAX_ID_NUMBER.tag -> {
                attributeTag = AttributeTag.TAX_ID_NUMBER
                name = mContext.getString(R.string.proof_of_identity_tax_id_number)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    value = rawValue
                    status = true
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            else -> {
                attributeTag = AttributeTag.UNKNOWN
                Log.e("UNKNOWN ATTRIBUTE: {${statement.attributeTag}}")
                name = mContext.getString(R.string.proof_of_identity_not_available)
                rawValue = null
                value = name
                status = false
            }
        }

        return ProofReveal(
            AttributeType.REVEAL_ATTRIBUTE, attributeTag, name, value, rawValue, status
        )
    }

    private fun convertSex(context: Context, value: String?): String? {
        return when (value) {
            "1" -> context.getString(R.string.identity_attribute_sex_male)
            "2" -> context.getString(R.string.identity_attribute_sex_female)
            else -> null
        }
    }

    private fun getCountryName(code: String): String {
        val local = Locale("", code)
        return local.displayCountry
    }

    private fun getDocType(context: Context, value: String): String {
        return when (value) {
            "1" -> context.getString(R.string.identity_attribute_doc_type_passport)
            "2" -> context.getString(R.string.identity_attribute_doc_type_national_id)
            "3" -> context.getString(R.string.identity_attribute_doc_type_driving_license)
            "4" -> context.getString(R.string.identity_attribute_doc_type_immigration_card)
            else -> context.getString(R.string.identity_attribute_na)
        }
    }
}