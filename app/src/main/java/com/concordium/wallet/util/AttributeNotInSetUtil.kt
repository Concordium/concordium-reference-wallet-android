package com.concordium.wallet.util

import android.content.Context
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.*
import java.util.*
import kotlin.collections.HashMap

object AttributeNotInSetUtil {

    fun getZeroProofKnowledge(
        statement: ProofOfIdentityStatement, data: HashMap<String, String>, euMembers: List<String>
    ): ProofZeroKnowledge {
        val attributeTag: AttributeTag?
        val name: String?
        val value: String?
        var rawValue: String?
        val status: Boolean?
        var description: String? = null
        val title: String?

        val mContext = App.appContext

        when (statement.attributeTag) {
            AttributeTag.COUNTRY_OF_RESIDENCE.tag -> {
                attributeTag = AttributeTag.COUNTRY_OF_RESIDENCE
                name =
                    mContext.getString(R.string.proof_of_identity_membership_country_of_residence)
                title = mContext.getString(
                    R.string.proof_of_identity_zero_title_country_of_residence
                )
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {

                    val countries = getCountriesString(statement.set!!)

                    if (isEuCountrySet(statement.set, euMembers)) {

                        value = mContext.getString(
                            R.string.proof_of_identity_membership_not_eu
                        )
                        status = !statement.set.contains(rawValue)
                        description = mContext.getString(
                            R.string.proof_of_identity_membership_country_not_in_eu_description
                        )

                    } else {

                        status = !statement.set.contains(rawValue)
                        value = mContext.getString(
                            R.string.proof_of_identity_membership_none,
                            statement.set.size.toString()
                        )
                        description = mContext.getString(
                            R.string.proof_of_identity_membership_country_not_in_set, countries
                        )

                    }
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.NATIONALITY.tag -> {
                attributeTag = AttributeTag.NATIONALITY
                name = mContext.getString(R.string.proof_of_identity_membership_nationality)
                title = mContext.getString(
                    R.string.proof_of_identity_zero_title_nationality
                )
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    val countries = getCountriesString(statement.set!!)

                    if (isEuCountrySet(statement.set, euMembers)) {

                        value = mContext.getString(
                            R.string.proof_of_identity_membership_not_eu
                        )
                        status = !statement.set.contains(rawValue)
                        description = mContext.getString(
                            R.string.proof_of_identity_membership_nationality_not_in_eu_description
                        )

                    } else {

                        status = !statement.set.contains(rawValue)
                        value = mContext.getString(
                            R.string.proof_of_identity_membership_none,
                            statement.set.size.toString()
                        )
                        description = mContext.getString(
                            R.string.proof_of_identity_membership_nationality_not_in_set, countries
                        )

                    }
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_TYPE.tag -> {
                attributeTag = AttributeTag.ID_DOC_TYPE
                name = mContext.getString(R.string.proof_of_identity_membership_document_type)
                title = mContext.getString(
                    R.string.proof_of_identity_zero_title_document_type
                )
                val types = getDocTypesString(mContext, statement.set!!)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    status = !statement.set.contains(rawValue)
                    value = mContext.getString(
                        R.string.proof_of_identity_type_none, statement.set.size.toString()
                    )
                    description = mContext.getString(
                        R.string.proof_of_identity_type_not_in_set, types
                    )
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                }
            }
            AttributeTag.ID_DOC_ISSUER.tag -> {
                attributeTag = AttributeTag.ID_DOC_ISSUER
                name = mContext.getString(R.string.proof_of_identity_membership_document_issuer)
                title = mContext.getString(
                    R.string.proof_of_identity_zero_title_document_issuer
                )
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    val countries = getCountriesString(statement.set!!)

                    status = !statement.set.contains(rawValue)
                    value = mContext.getString(
                        R.string.proof_of_identity_issuers_none, statement.set.size.toString()
                    )
                    description = mContext.getString(
                        R.string.proof_of_identity_issuers_not_in_set, countries
                    )

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
                title = name
                status = false
            }
        }

        return ProofZeroKnowledge(
            AttributeType.ATTRIBUTE_IN_SET,
            attributeTag,
            name,
            value,
            rawValue,
            description,
            title,
            status
        )
    }

    private fun isEuCountrySet(countries: List<String>?, euMembers: List<String>): Boolean {
        if (countries.isNullOrEmpty() || countries.size != euMembers.size) {
            return false
        }
        if (euMembers.containsAll(countries)) {
            return true
        }
        return false

    }

    private fun getCountriesString(statement: List<String>): String {
        var countries = ""
        statement.forEach {
            countries = if (countries.isNotEmpty()) {
                "$countries, ${getCountryName(it)}"
            } else getCountryName(it)
        }
        return countries
    }

    private fun getCountryName(code: String): String {
        val local = Locale("", code)
        return local.displayCountry
    }

    private fun getDocTypesString(mContext: Context, statement: List<String>): String {
        var docTypes = ""
        statement.forEach {
            docTypes = if (docTypes.isNotEmpty()) {
                "$docTypes, ${getDocType(mContext, it)}"
            } else getDocType(mContext, it)
        }
        return docTypes
    }

    private fun getDocType(context: Context, value: String): String {
        return when (value) {
            "0" -> context.getString(R.string.identity_attribute_doc_type_not_available)
            "1" -> context.getString(R.string.identity_attribute_doc_type_passport)
            "2" -> context.getString(R.string.identity_attribute_doc_type_national_id)
            "3" -> context.getString(R.string.identity_attribute_doc_type_driving_license)
            "4" -> context.getString(R.string.identity_attribute_doc_type_immigration_card)
            else -> context.getString(R.string.identity_attribute_na)
        }
    }
}