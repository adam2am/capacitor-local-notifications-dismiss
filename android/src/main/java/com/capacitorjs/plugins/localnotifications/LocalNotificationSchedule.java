package com.capacitorjs.plugins.localnotifications;

import android.text.format.DateUtils;
import com.getcapacitor.JSObject;
import com.google.gson.annotations.SerializedName;
import java.util.Calendar;
import java.util.Date;

public class LocalNotificationSchedule {

    public static final String JS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private Date at;
    private Boolean repeats;
    private String every;
    private Integer count = 1;

    private ScheduleOn on;

    @SerializedName("allowWhileIdle")
    private Boolean allowWhileIdle;

    /** No-arg constructor required for Gson deserialization */
    public LocalNotificationSchedule() {}

    /**
     * Nested class representing cron-like schedule rules
     */
    public static class ScheduleOn {
        public Integer year;
        public Integer month;
        public Integer day;
        public Integer weekday;
        public Integer hour;
        public Integer minute;
        public Integer second;
    }

    public ScheduleOn getOn() {
        return on;
    }

    public JSObject getOnObj() {
        JSObject onJson = new JSObject();
        if (on != null) {
            if (on.year != null) onJson.put("year", on.year);
            if (on.month != null) onJson.put("month", on.month);
            if (on.day != null) onJson.put("day", on.day);
            if (on.weekday != null) onJson.put("weekday", on.weekday);
            if (on.hour != null) onJson.put("hour", on.hour);
            if (on.minute != null) onJson.put("minute", on.minute);
            if (on.second != null) onJson.put("second", on.second);
        }
        return onJson;
    }

    public void setOn(ScheduleOn on) {
        this.on = on;
    }

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public Boolean getRepeats() {
        return repeats;
    }

    public void setRepeats(Boolean repeats) {
        this.repeats = repeats;
    }

    public String getEvery() {
        return every;
    }

    public void setEvery(String every) {
        this.every = every;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public boolean allowWhileIdle() {
        return Boolean.TRUE.equals(this.allowWhileIdle);
    }

    public boolean isRepeating() {
        return Boolean.TRUE.equals(this.repeats);
    }

    public boolean isRemovable() {
        if (every == null && on == null) {
            if (at != null) {
                return !isRepeating();
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Get constant long value representing specific interval of time (weeks, days etc.)
     */
    public Long getEveryInterval() {
        if (every == null) {
            return null;
        }
        int countVal = (count == null) ? 1 : count;
        switch (every) {
            case "year":
                // This case is just approximation as not all years have the same number of days
                return countVal * DateUtils.WEEK_IN_MILLIS * 52;
            case "month":
                // This case is just approximation as months have different number of days
                return countVal * 30 * DateUtils.DAY_IN_MILLIS;
            case "two-weeks":
                return countVal * 2 * DateUtils.WEEK_IN_MILLIS;
            case "week":
                return countVal * DateUtils.WEEK_IN_MILLIS;
            case "day":
                return countVal * DateUtils.DAY_IN_MILLIS;
            case "hour":
                return countVal * DateUtils.HOUR_IN_MILLIS;
            case "minute":
                return countVal * DateUtils.MINUTE_IN_MILLIS;
            case "second":
                return countVal * DateUtils.SECOND_IN_MILLIS;
            default:
                return null;
        }
    }

    /**
     * Get next trigger time based on calendar and current time
     *
     * @param currentTime - current time that will be used to calculate next trigger
     * @return millisecond trigger
     */
    public Long getNextOnSchedule(Date currentTime) {
        if (this.on == null) {
            return null;
        }

        Calendar now = Calendar.getInstance();
        now.setTime(currentTime);
        now.set(Calendar.MILLISECOND, 0);

        Calendar next = Calendar.getInstance();
        next.setTime(currentTime);
        next.set(Calendar.MILLISECOND, 0);

        // Determine which unit is the smallest specified (most granular)
        int smallestUnit = -1;

        // Apply specified calendar fields
        if (on.year != null) {
            next.set(Calendar.YEAR, on.year);
            smallestUnit = Calendar.YEAR;
        }
        if (on.month != null) {
            // Gson/JSON uses 1-based months, Calendar uses 0-based
            next.set(Calendar.MONTH, on.month - 1);
            smallestUnit = Calendar.MONTH;
        }
        if (on.day != null) {
            next.set(Calendar.DAY_OF_MONTH, on.day);
            smallestUnit = Calendar.DAY_OF_MONTH;
        }
        if (on.weekday != null) {
            next.set(Calendar.DAY_OF_WEEK, on.weekday);
            smallestUnit = Calendar.DAY_OF_WEEK;
        }
        if (on.hour != null) {
            next.set(Calendar.HOUR_OF_DAY, on.hour);
            smallestUnit = Calendar.HOUR_OF_DAY;
        }
        if (on.minute != null) {
            next.set(Calendar.MINUTE, on.minute);
            smallestUnit = Calendar.MINUTE;
        }
        if (on.second != null) {
            next.set(Calendar.SECOND, on.second);
            smallestUnit = Calendar.SECOND;
        }

        // If the calculated time is in the past, advance to next occurrence
        if (!next.after(now) && smallestUnit != -1) {
            int incrementUnit = getIncrementUnit(smallestUnit);
            if (incrementUnit != -1) {
                next.add(incrementUnit, 1);
            }
        }

        return next.getTimeInMillis();
    }

    /**
     * Determine which calendar unit to increment when rescheduling
     */
    private int getIncrementUnit(int smallestUnit) {
        if (smallestUnit == Calendar.YEAR || smallestUnit == Calendar.MONTH) {
            return Calendar.YEAR;
        } else if (smallestUnit == Calendar.DAY_OF_MONTH) {
            return Calendar.MONTH;
        } else if (smallestUnit == Calendar.DAY_OF_WEEK) {
            return Calendar.WEEK_OF_MONTH;
        } else if (smallestUnit == Calendar.HOUR_OF_DAY) {
            return Calendar.DAY_OF_MONTH;
        } else if (smallestUnit == Calendar.MINUTE) {
            return Calendar.HOUR_OF_DAY;
        } else if (smallestUnit == Calendar.SECOND) {
            return Calendar.MINUTE;
        }
        return -1;
    }
}
