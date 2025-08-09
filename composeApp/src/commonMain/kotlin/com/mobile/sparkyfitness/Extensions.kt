package com.mobile.sparkyfitness

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char


val displayFormat = LocalDateTime.Format {
    //h:mma MM-dd-yyyy
    amPmHour();
    char(':');
    minute();
    amPmMarker(
        "AM",
        "PM"
    );
    char(' ');
    monthName(MonthNames.ENGLISH_ABBREVIATED);
    char(' ');
    dayOfMonth();
    chars(", ");
    year()
}