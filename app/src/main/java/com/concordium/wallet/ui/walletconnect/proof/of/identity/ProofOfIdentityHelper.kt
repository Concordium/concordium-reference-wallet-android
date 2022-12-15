package com.concordium.wallet.ui.walletconnect.proof.of.identity

import android.content.Context
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.*
import com.concordium.wallet.util.DateTimeUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList

class ProofOfIdentityHelper(
    private val proofOfIdentity: ProofOfIdentity,
    private val data: List<IdentityAttribute>,
) {

    companion object {
        private const val MIN_DATE = "18000101"
        private const val MAX_DATE = "99990101"
        private val EU_MEMBERS = listOf("FR", "GR")
    }

    private val proofsReveal = ArrayList<ProofReveal>()
    private val proofsZeroKnowledge = ArrayList<ProofZeroKnowledge>()

    fun getProofs(): Proofs? {

        for (statement in proofOfIdentity.statement!!) {
            when (statement.type) {
                AttributeType.REVEAL_ATTRIBUTE -> {
                    revealProof(statement)?.let {
                        proofsReveal.add(it)
                    }
                }
                AttributeType.ATTRIBUTE_IN_SET -> {
                    attributeInSet(statement, true)?.let {
                        proofsZeroKnowledge.add(it)
                    }
                }
                AttributeType.ATTRIBUTE_NOT_IN_SET -> {
                    attributeInSet(statement, false)?.let {
                        proofsZeroKnowledge.add(it)
                    }
                }
                AttributeType.ATTRIBUTE_IN_RANGE -> {
                    attributeInRange(statement)?.let {
                        proofsZeroKnowledge.add(it)
                    }
                }
                else -> return null
            }
        }
        return Proofs(proofsReveal, proofsZeroKnowledge)

    }

    private fun attributeInRange(
        statement: ProofOfIdentityStatement
    ): ProofZeroKnowledge? {
        val attributeTag: AttributeTag?
        val name: String?
        val value: String?
        val rawValue: String?
        val status: Boolean?
        var description: String? = null

        val mContext = App.appContext

        when (statement.attributeTag) {
            AttributeTag.DOB -> {
                attributeTag = AttributeTag.DOB
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                val dob = dateStringToDate(identityAttribute.value)
                val today = LocalDate.now()
                val age = differenceInYears(dob, today)
                val upperDate = dateStringToDate(statement.upper!!)
                val lowerDate = dateStringToDate(statement.lower!!)
                val upper = if (today < upperDate) today else upperDate
                if (isAgeStatement(statement)) {
                    name = mContext.getString(R.string.proof_of_identity_range_age)
                    val ageMin = today.year - upper.year
                    val ageMax =
                        today.year - getYearFromDateString(addDays(statement.lower, -1)) - 1

                    if (statement.lower == MIN_DATE) {
                        value = mContext.getString(
                            R.string.proof_of_identity_minimum_age, ageMin.toString()
                        )
                        status = ageMin <= age
                        description = mContext.getString(
                            R.string.proof_of_identity_minimum_age_description,
                            dateToFormattedString(upper)
                        )
                        //  return t('ageMin', { age: ageMin });
                    } else if (upperDate > today) {
                        value = mContext.getString(
                            R.string.proof_of_identity_maximum_age, ageMax.toString()
                        )
                        status = ageMax >= age
                        description = mContext.getString(
                            R.string.proof_of_identity_maximum_age_description,
                            dateToFormattedString(upperDate)
                        )
                        //return t('ageMax', { age: ageMax });
                    } else if (ageMin == ageMax) {
                        value = mContext.getString(
                            R.string.proof_of_identity_exact_age, age.toString()
                        )
                        status = ageMin == age
                        description = mContext.getString(
                            R.string.proof_of_identity_exact_age_description,
                            dateToFormattedString(dob)
                        )
                        //return t('ageExact', { age: ageMin });
                    } else {
                        value = mContext.getString(
                            R.string.proof_of_identity_between_age,
                            ageMin.toString(),
                            ageMax.toString()
                        )
                        status = age in (ageMin + 1) until ageMax
                        description = mContext.getString(
                            R.string.proof_of_identity_between_age_description,
                            dateToFormattedString(lowerDate),
                            dateToFormattedString(upperDate)
                        )
                        //return t('ageBetween', { ageMin, ageMax });
                    }

                } else {
                    name = mContext.getString(R.string.proof_of_identity_dob)

                    if (statement.lower === MIN_DATE) {
                        value = mContext.getString(
                            R.string.proof_of_identity_after_date, dateToFormattedString(upper)
                        )
                        status = dob.isAfter(upper)
                        //return t('dateAfter', { dateString: maxDateString });
                    } else if (upperDate > today) {
                        value = mContext.getString(
                            R.string.proof_of_identity_before_date, dateToFormattedString(lowerDate)
                        )
                        status = dob.isBefore(upper)
                        //return t('dateBefore', { dateString: minDateString });
                    } else {
                        value = mContext.getString(
                            R.string.proof_of_identity_between_dates,
                            dateToFormattedString(lowerDate),
                            dateToFormattedString(upper)
                        )
                        status = lowerDate < dob && dob < upper
                        //return t('dateBetween', { minDateString, maxDateString });
                    }

                }
            }
            AttributeTag.ID_DOC_ISSUED_AT -> {
                attributeTag = AttributeTag.ID_DOC_ISSUED_AT
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                val date = dateStringToDate(identityAttribute.value)
                name = mContext.getString(R.string.proof_of_identity_range_id_issued)
                val minDate = dateStringToDate(statement.lower!!)
                val maxDate = dateStringToDate(statement.upper!!)
                if (statement.lower == MIN_DATE) {

                    value = mContext.getString(
                        R.string.proof_of_identity_after_date, dateToFormattedString(maxDate)
                    )
                    status = date.isAfter(maxDate)

                    //return t('dateAfter', { dateString: maxDateString });
                } else if (statement.upper == MAX_DATE) {
                    value = mContext.getString(
                        R.string.proof_of_identity_before_date, dateToFormattedString(minDate)
                    )
                    status = date.isBefore(minDate)
                    //return t('dateBefore', { dateString: minDateString });
                } else {
                    value = mContext.getString(
                        R.string.proof_of_identity_between_dates,
                        dateToFormattedString(minDate),
                        dateToFormattedString(maxDate)
                    )
                    status = minDate < date && date < maxDate
                    //return t('dateBetween', { minDateString, maxDateString })
                }
            }
            AttributeTag.ID_DOC_EXPIRES_AT -> {
                attributeTag = AttributeTag.ID_DOC_EXPIRES_AT
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                val date = dateStringToDate(identityAttribute.value)
                name = mContext.getString(R.string.proof_of_identity_range_id_expires)
                val minDate = dateStringToDate(statement.lower!!)
                val maxDate = dateStringToDate(statement.upper!!)
                if (statement.lower == MIN_DATE) {

                    value = mContext.getString(
                        R.string.proof_of_identity_after_date, dateToFormattedString(maxDate)
                    )
                    status = date.isAfter(maxDate)

                    //return t('dateAfter', { dateString: maxDateString });
                } else if (statement.upper == MAX_DATE) {
                    value = mContext.getString(
                        R.string.proof_of_identity_before_date, dateToFormattedString(minDate)
                    )
                    status = date.isBefore(minDate)
                    //return t('dateBefore', { dateString: minDateString });
                } else {
                    value = mContext.getString(
                        R.string.proof_of_identity_between_dates,
                        dateToFormattedString(minDate),
                        dateToFormattedString(maxDate)
                    )
                    status = minDate < date && date < maxDate
                    //return t('dateBetween', { minDateString, maxDateString })
                }
            }
            else -> return null
        }

        return ProofZeroKnowledge(
            AttributeType.ATTRIBUTE_IN_RANGE,
            attributeTag,
            name,
            value,
            description,
            rawValue,
            status
        )
    }

    private fun getYearFromDateString(timeStr: String): Int {
        return timeStr.substring(0, 4).toInt()
    }

    private fun attributeInSet(
        statement: ProofOfIdentityStatement, inSet: Boolean
    ): ProofZeroKnowledge? {
        val attributeTag: AttributeTag?
        var name: String?
        var value: String? = null
        var rawValue: String? = null
        var status: Boolean? = null
        var description: String? = null

        val mContext = App.appContext

        when (statement.attributeTag) {
            AttributeTag.COUNTRY_OF_RESIDENCE -> {
                attributeTag = AttributeTag.COUNTRY_OF_RESIDENCE
                name =
                    mContext.getString(R.string.proof_of_identity_membership_country_of_residence)
                if(isEuCountrySet(statement.set)){

                }
            }
            AttributeTag.NATIONALITY -> {
                attributeTag = AttributeTag.NATIONALITY
                name = mContext.getString(R.string.proof_of_identity_membership_nationality)
            }
            AttributeTag.ID_DOC_TYPE -> {
                attributeTag = AttributeTag.ID_DOC_TYPE
                name = mContext.getString(R.string.proof_of_identity_membership_document_type)
            }
            AttributeTag.ID_DOC_ISSUER -> {
                attributeTag = AttributeTag.ID_DOC_ISSUER
                name = mContext.getString(R.string.proof_of_identity_membership_document_issuer)
            }
            else -> return null
        }

        val type = if (inSet) AttributeType.ATTRIBUTE_IN_SET else AttributeType.ATTRIBUTE_NOT_IN_SET

        return ProofZeroKnowledge(type, attributeTag, name, value, rawValue, description, status)
    }

    private fun revealProof(statement: ProofOfIdentityStatement): ProofReveal? {
        val attributeTag: AttributeTag?
        val name: String?
        val value: String?
        val rawValue: String?
        val status: Boolean?

        val mContext = App.appContext

        when (statement.attributeTag) {
            AttributeTag.FIRST_NAME -> {
                attributeTag = AttributeTag.FIRST_NAME
                name = mContext.getString(R.string.proof_of_identity_first_name)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = rawValue
                status = rawValue.isEmpty()
            }

            AttributeTag.LAST_NAME -> {
                attributeTag = AttributeTag.LAST_NAME
                name = mContext.getString(R.string.proof_of_identity_last_name)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = rawValue
                status = value.isEmpty()
            }
            AttributeTag.SEX -> {
                attributeTag = AttributeTag.SEX
                name = mContext.getString(R.string.proof_of_identity_sex)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = convertSex(mContext, rawValue)
                status = rawValue.isEmpty()
            }
            AttributeTag.DOB -> {
                attributeTag = AttributeTag.DOB
                name = mContext.getString(R.string.proof_of_identity_date_of_birth)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = DateTimeUtil.convertLongDate(rawValue)
                status = value.isEmpty()
            }
            AttributeTag.COUNTRY_OF_RESIDENCE -> {
                attributeTag = AttributeTag.COUNTRY_OF_RESIDENCE
                name = mContext.getString(R.string.proof_of_identity_country_of_residence)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = getCountryName(rawValue)
                status = value.isEmpty()
            }
            AttributeTag.NATIONALITY -> {
                attributeTag = AttributeTag.NATIONALITY
                name = mContext.getString(R.string.proof_of_identity_nationality)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = getCountryName(rawValue)
                status = value.isEmpty()
            }
            AttributeTag.ID_DOC_TYPE -> {
                attributeTag = AttributeTag.ID_DOC_TYPE
                name = mContext.getString(R.string.proof_of_identity_id_document_type)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = getDocType(mContext, rawValue)
                status = value.isEmpty()
            }
            AttributeTag.ID_DOC_NUMBER -> {
                attributeTag = AttributeTag.ID_DOC_NUMBER
                name = mContext.getString(R.string.proof_of_identity_id_document_number)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = rawValue
                status = value.isEmpty()
            }
            AttributeTag.ID_DOC_ISSUER -> {
                attributeTag = AttributeTag.ID_DOC_ISSUER
                name = mContext.getString(R.string.proof_of_identity_id_document_issuer)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = getCountryName(rawValue)
                status = value.isEmpty()
            }
            AttributeTag.ID_DOC_ISSUED_AT -> {
                attributeTag = AttributeTag.ID_DOC_ISSUED_AT
                name = mContext.getString(R.string.proof_of_identity_id_document_issued_at)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = DateTimeUtil.convertLongDate(rawValue)
                status = value.isEmpty()
            }
            AttributeTag.ID_DOC_EXPIRES_AT -> {
                attributeTag = AttributeTag.ID_DOC_EXPIRES_AT
                name = mContext.getString(R.string.proof_of_identity_id_document_expires_at)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = DateTimeUtil.convertLongDate(rawValue)
                status = value.isEmpty()
            }
            AttributeTag.NATIONAL_ID_NUMBER -> {
                attributeTag = AttributeTag.NATIONAL_ID_NUMBER
                name = mContext.getString(R.string.proof_of_identity_national_id_number)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = rawValue
                status = value.isEmpty()
            }
            AttributeTag.TAX_ID_NUMBER -> {
                attributeTag = AttributeTag.TAX_ID_NUMBER
                name = mContext.getString(R.string.proof_of_identity_tax_id_number)
                val identityAttribute = data.first { it.name == attributeTag.tag }
                rawValue = identityAttribute.value
                value = rawValue
                status = value.isEmpty()
            }
            else -> return null
        }

        return ProofReveal(
            AttributeType.REVEAL_ATTRIBUTE, attributeTag, name, value, rawValue, status
        )
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
            "1" -> context.getString(R.string.identity_attribute_doc_type_passport)
            "2" -> context.getString(R.string.identity_attribute_doc_type_national_id)
            "3" -> context.getString(R.string.identity_attribute_doc_type_driving_license)
            "4" -> context.getString(R.string.identity_attribute_doc_type_immigration_card)
            else -> context.getString(R.string.identity_attribute_na)
        }
    }

    private fun isAgeStatement(statement: ProofOfIdentityStatement): Boolean {
        val formatter = DateTimeFormatter.ofPattern("YYYYMMDD")
        val current = LocalDate.now().format(formatter)

        val isYearOffsetUpper = (statement.upper?.substring(0, 4) ?: "") == current.substring(0, 4)
        val isYearOffsetLower =
            statement.lower?.let { addDays(it, -1).substring(4) } == current.substring(4)
        if (statement.lower == MIN_DATE) {
            return isYearOffsetUpper
        }
        if (dateStringToDate(statement.upper!!).isAfter(LocalDate.now())) {
            return isYearOffsetLower
        }

        return isYearOffsetUpper && isYearOffsetLower;

    }

    /**
     * Given YYYYMMDD return YYYYMMDD + x day(s).
     */
    private fun addDays(date: String, days: Int): String {
        val d = dateStringToDate(date)
        return if (days < 0) {
            dateToDateString(d.minusDays((-days).toLong()))
        } else {
            dateToDateString(d.plusDays(days.toLong()))
        }
    }

    /**
     * Turns a YYYYMMDD string into a date object
     */
    private fun dateStringToDate(date: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("YYYYMMDD")
        return LocalDate.parse(date, formatter)
    }

    /**
     * Turns a date object into a YYYYMMDD string
     */
    private fun dateToDateString(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("YYYYMMDD")
        return date.format(formatter)
    }


    private fun dateToFormattedString(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("YYYY-MM-DD")
        return date.format(formatter)
    }

    private fun differenceInYears(lower: LocalDate, upper: LocalDate): Int {
        return ChronoUnit.YEARS.between(
            lower, upper
        ).toInt()
    }

    private fun isEuCountrySet(countries: List<String>?): Boolean{
        if(countries.isNullOrEmpty()){
            return false
        }
        if(countries.size != EU_MEMBERS.size){
            return false
        }
        if(EU_MEMBERS.containsAll(countries)){
            return true
        }
        return false

    }


}