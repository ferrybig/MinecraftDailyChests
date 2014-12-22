package me.ferrybig.java.bukkit.plugins.dailychests;

public class TimeConvertor {

    private TimeConvertor() {
    }

    private enum TimeUnits {

        MILISECONDS("miliseconds"),
        SECONDS("seconds", 1000, MILISECONDS),
        MINUTES("minutes", 60, SECONDS),
        HOURS("hours", 60, MINUTES),
        DAYS("days", 24, HOURS),
        WEEKS("weeks", 7, DAYS),
        MONTHS("months", 4, WEEKS),
        YEARS("years", 365, DAYS);

        private TimeUnits(String friendlyName, long commonDivider,
            TimeUnits parent) {
            this(friendlyName, commonDivider * parent.getCommonDivider());
        }

        private TimeUnits(String friendlyName, long commonDivider) {
            this.friendlyName = friendlyName;
            this.commonDivider = commonDivider;
        }

        private TimeUnits(String friendlyName) {
            this(friendlyName, 1);
        }

        private final String friendlyName;
        private final long commonDivider;

        public long getCommonDivider() {
            return commonDivider;
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }

    public static String convertTime(long time,
        int maxStringComponents, String delimiter) {
        StringBuilder builder = new StringBuilder();
        final TimeUnits[] values = TimeUnits.values();
        long tempTime = 0;
        long tempTimeBuilding;
        for (int i = 0; i < maxStringComponents; i++) {
            TimeUnits lowestUnit = null;
            for (TimeUnits unit : values) {
                tempTimeBuilding = time / unit.getCommonDivider();
                if (tempTimeBuilding > 0) {
                    tempTime = tempTimeBuilding;
                    lowestUnit = unit;
                } else {
                    break;
                }
            }
            if (lowestUnit == null) {
                break;
            }
            if (i != 0) {
                builder.append(delimiter);
            }
            time -= tempTime * lowestUnit.getCommonDivider();
            builder.append(tempTime).append(' ')
                .append(lowestUnit.getFriendlyName());
        }
        return builder.toString();
    }

    public static void main(String... args) {
        System.out.println(convertTime(1, 3, ", "));
        System.out.println(convertTime(10, 3, ", "));
        System.out.println(convertTime(100, 3, ", "));
        System.out.println(convertTime(1000, 3, ", "));
        System.out.println(convertTime(10000, 3, ", "));
        System.out.println(convertTime(100000, 3, ", "));
        System.out.println(convertTime(1000000, 3, ", "));
        System.out.println(convertTime(345127364, 3, ", "));
    }
}
