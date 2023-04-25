package com.concordium.wallet.data.util

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.util.DateTimeUtil
import java.util.*

object IdentityAttributeConverterUtil {

    fun convertAttributeValue(
        context: Context,
        attribute: Pair<String, String>
    ): Pair<String, String> {
        return when (attribute.first) {
            "firstName" ->
                Pair(
                    context.getString(R.string.identity_attribute_first_name),
                    attribute.second
                )

            "lastName" ->
                Pair(
                    context.getString(R.string.identity_attribute_last_name),
                    attribute.second
                )

            "sex" ->
                Pair(
                    context.getString(R.string.identity_attribute_sex),
                    convertSex(context, attribute.second)
                )

            "dob" ->
                Pair(
                    context.getString(R.string.identity_attribute_birth_date),
                    DateTimeUtil.convertLongDate(attribute.second)
                )

            "countryOfResidence" ->
                Pair(
                    context.getString(R.string.identity_attribute_country_residence),
                    getCountryName(attribute.second)
                )

            "nationality" ->
                Pair(
                    context.getString(R.string.identity_attribute_nationality),
                    getCountryName(attribute.second)
                )

            "idDocType" ->
                Pair(
                    context.getString(R.string.identity_attribute_doc_type),
                    getDocType(context, attribute.second)
                )

            "idDocNo" ->
                Pair(
                    context.getString(R.string.identity_attribute_doc_no),
                    attribute.second
                )

            "idDocIssuer" ->
                Pair(
                    context.getString(R.string.identity_attribute_doc_issuer),
                    getCountryName(attribute.second)
                )

            "idDocIssuedAt" ->
                Pair(
                    context.getString(R.string.identity_attribute_doc_issued_at),
                    DateTimeUtil.convertLongDate(attribute.second)
                )

            "idDocExpiresAt" ->
                Pair(
                    context.getString(R.string.identity_attribute_doc_expires_at),
                    DateTimeUtil.convertLongDate(attribute.second)
                )

            "nationalIdNo" ->
                Pair(
                    context.getString(R.string.identity_attribute_national_id_no),
                    attribute.second
                )

            "taxIdNo" ->
                Pair(
                    context.getString(R.string.identity_attribute_tax_id_no),
                    attribute.second
                )

            else -> Pair("", "")
        }
    }

    private fun convertSex(context: Context, value: String): String {
        return when (value) {
            "1" -> context.getString(R.string.identity_attribute_sex_male)
            "2" -> context.getString(R.string.identity_attribute_sex_female)
            else -> context.getString(R.string.identity_attribute_na)
        }
    }

    private fun getCountryName(code: String): String {
        val local = Locale("", code)
        return local.displayCountry
    }

    private fun getDocType(context: Context, value: String): String {
        return when (value) {
            "0" -> context.getString(R.string.identity_attribute_na)
            "1" -> context.getString(R.string.identity_attribute_doc_type_passport)
            "2" -> context.getString(R.string.identity_attribute_doc_type_national_id)
            "3" -> context.getString(R.string.identity_attribute_doc_type_driving_license)
            "4" -> context.getString(R.string.identity_attribute_doc_type_immigration_card)
            else -> value
        }
    }
}
