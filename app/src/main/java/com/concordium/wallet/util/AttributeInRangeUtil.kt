package com.concordium.wallet.util

import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object AttributeInRangeUtil {

    private const val MIN_DATE = "18000101"
    private const val MAX_DATE = "99990101"

    fun attributeInRange(
        statement: ProofOfIdentityStatement, data: HashMap<String, String>
    ): ProofZeroKnowledge {
        val attributeTag: AttributeTag?
        val name: String?
        val value: String?
        var rawValue: String?
        val status: Boolean?
        val description: String?
        val title: String?

        val mContext = App.appContext

        when (statement.attributeTag) {
            AttributeTag.DOB.tag -> {
                attributeTag = AttributeTag.DOB

                val ageStatement = isAgeStatement(statement)
                name =
                    if (ageStatement) mContext.getString(R.string.proof_of_identity_range_age) else mContext.getString(
                        R.string.proof_of_identity_dob
                    )

                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    val dob = dateStringToDate(rawValue)
                    val today = dateStringToDate(getPastDate(0))
                    val age = differenceInYears(dob, today)
                    val upperDate = dateStringToDate(statement.upper!!)
                    val lowerDate = dateStringToDate(statement.lower!!)
                    val upper = if (today < upperDate) today else upperDate

                    if (ageStatement) {
                        title = mContext.getString(
                            R.string.proof_of_identity_zero_title_age
                        )
                        val ageMin = today.year - upper.year
                        val ageMax = today.year - getYearFromDateString(statement.lower)

                        if (statement.lower == MIN_DATE) {
                            value = mContext.getString(
                                R.string.proof_of_identity_minimum_age, ageMin.toString()
                            )
                            status = ageMin < age
                            description = mContext.getString(
                                R.string.proof_of_identity_minimum_age_description,
                                dateToFormattedString(upper)
                            )
                        } else if (upperDate > today) {
                            value = mContext.getString(
                                R.string.proof_of_identity_maximum_age, ageMax.toString()
                            )
                            status = ageMax > age
                            description = mContext.getString(
                                R.string.proof_of_identity_maximum_age_description,
                                dateToFormattedString(upperDate)
                            )
                        } else if (ageMin == ageMax) {
                            value = mContext.getString(
                                R.string.proof_of_identity_exact_age, age.toString()
                            )
                            status = ageMin == age
                            description = mContext.getString(
                                R.string.proof_of_identity_exact_age_description,
                                dateToFormattedString(dob)
                            )
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
                        }

                    } else {
                        title = mContext.getString(
                            R.string.proof_of_identity_zero_title_dob
                        )

                        if (statement.lower == MIN_DATE) {
                            value = mContext.getString(
                                R.string.proof_of_identity_after_date, dateToFormattedString(upper)
                            )
                            status = dob.isAfter(upper)
                            description = null
                        } else if (upperDate > today) {
                            value = mContext.getString(
                                R.string.proof_of_identity_before_date,
                                dateToFormattedString(lowerDate)
                            )
                            status = dob.isAfter(lowerDate)
                            description = null
                        } else {
                            value = mContext.getString(
                                R.string.proof_of_identity_between_dates,
                                dateToFormattedString(lowerDate),
                                dateToFormattedString(upper)
                            )
                            status = lowerDate < dob && dob < upper
                            description = null
                        }
                    }
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                    description = null
                    title = mContext.getString(R.string.proof_of_identity_not_available)
                }
            }
            AttributeTag.ID_DOC_ISSUED_AT.tag -> {
                attributeTag = AttributeTag.ID_DOC_ISSUED_AT
                title = mContext.getString(
                    R.string.proof_of_identity_zero_title_id_validity
                )
                name = mContext.getString(R.string.proof_of_identity_range_id_issued)
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    val date = dateStringToDate(rawValue)
                    val minDate = dateStringToDate(statement.lower!!)
                    val maxDate = dateStringToDate(statement.upper!!)
                    if (statement.lower == MIN_DATE) {

                        value = mContext.getString(
                            R.string.proof_of_identity_after_date, dateToFormattedString(maxDate)
                        )
                        status = date.isAfter(maxDate)
                        description = null
                    } else if (statement.upper == MAX_DATE) {
                        value = mContext.getString(
                            R.string.proof_of_identity_before_date, dateToFormattedString(minDate)
                        )
                        status = date.isAfter(minDate)
                        description = null
                    } else {
                        value = mContext.getString(
                            R.string.proof_of_identity_between_dates,
                            dateToFormattedString(minDate),
                            dateToFormattedString(maxDate)
                        )
                        status = minDate < date && date < maxDate
                        description = null
                    }
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                    description = null
                }
            }
            AttributeTag.ID_DOC_EXPIRES_AT.tag -> {
                attributeTag = AttributeTag.ID_DOC_EXPIRES_AT
                name = mContext.getString(R.string.proof_of_identity_range_id_expires)
                title = mContext.getString(
                    R.string.proof_of_identity_zero_title_id_validity
                )
                rawValue = data[attributeTag.tag]
                if (!rawValue.isNullOrEmpty()) {
                    val date = dateStringToDate(rawValue)
                    val minDate = dateStringToDate(statement.lower!!)
                    val maxDate = dateStringToDate(statement.upper!!)
                    if (statement.lower == MIN_DATE) {

                        value = mContext.getString(
                            R.string.proof_of_identity_after_date, dateToFormattedString(maxDate)
                        )
                        status = date.isAfter(maxDate)
                        description = null
                    } else if (statement.upper == MAX_DATE) {
                        value = mContext.getString(
                            R.string.proof_of_identity_before_date, dateToFormattedString(minDate)
                        )
                        status = date.isAfter(minDate)
                        description = null
                    } else {
                        value = mContext.getString(
                            R.string.proof_of_identity_between_dates,
                            dateToFormattedString(minDate),
                            dateToFormattedString(maxDate)
                        )
                        status = minDate < date && date < maxDate
                        description = null
                    }
                } else {
                    rawValue = null
                    value = mContext.getString(R.string.proof_of_identity_not_available)
                    status = false
                    description = null
                }
            }
            else -> {
                attributeTag = AttributeTag.UNKNOWN
                Log.e("UNKNOWN ATTRIBUTE: {${statement.attributeTag}}")
                name = mContext.getString(R.string.proof_of_identity_not_available)
                rawValue = null
                value = name
                status = false
                title = name
                description = null
            }
        }

        return ProofZeroKnowledge(
            AttributeType.ATTRIBUTE_IN_RANGE,
            attributeTag,
            name,
            value,
            rawValue,
            description,
            title,
            status
        )
    }

    private fun isAgeStatement(statement: ProofOfIdentityStatement): Boolean {
        val current = getPastDate(0)
        val isYearOffsetUpper = statement.upper?.substring(4) == current.substring(4)
        val isYearOffsetLower = statement.lower!!.substring(4) == current.substring(4)
        if (statement.lower == MIN_DATE) {
            return isYearOffsetUpper
        }
        if (dateStringToDate(statement.upper!!).isAfter(dateStringToDate(current))) {
            return isYearOffsetLower
        }

        return isYearOffsetUpper && isYearOffsetLower

    }

    /**
     * Given a number x, return the date string for x years ago.
     * @returns YYYYMMDD for x years ago today in local time.
     */
    private fun getPastDate(yearsAgo: Int): String {
        var current = LocalDate.now()
        current = current.minusYears(yearsAgo.toLong())
        return dateToDateString(current);
    }

    private fun getYearFromDateString(timeStr: String): Int {
        return timeStr.substring(0, 4).toInt()
    }

    /**
     * Given yyyyMMdd return yyyyMMdd + x day(s).
     */
    private fun addDays(date: String, days: Int): String {
        val d = dateStringToDate(date)
        return if (days < 0) {
            dateToDateString(d.minusDays(days.toLong()))
        } else {
            dateToDateString(d.plusDays(days.toLong()))
        }
    }

    /**
     * Turns a yyyyMMdd string into a date object
     */
    private fun dateStringToDate(date: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return LocalDate.parse(date, formatter)
    }

    /**
     * Turns a date object into a yyyyMMdd string
     */
    private fun dateToDateString(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return date.format(formatter)
    }


    private fun dateToFormattedString(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return date.format(formatter)
    }

    private fun differenceInYears(lower: LocalDate, upper: LocalDate): Int {
        return ChronoUnit.YEARS.between(
            lower, upper
        ).toInt()
    }
}