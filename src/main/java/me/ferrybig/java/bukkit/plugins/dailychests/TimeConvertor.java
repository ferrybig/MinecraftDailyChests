package me.ferrybig.java.bukkit.plugins.dailychests;

public class TimeConvertor {

	private TimeConvertor() {
	}

	private enum TimeUnits {

		MILISECONDS("miliseconds", "milisecond"),
		SECONDS("seconds", "second", 1000, MILISECONDS),
		MINUTES("minutes", "minute", 60, SECONDS),
		HOURS("hours", "hour", 60, MINUTES),
		DAYS("days", "day", 24, HOURS),
		WEEKS("weeks", "week", 7, DAYS),
		MONTHS("months", "month", 4, WEEKS),
		YEARS("years", "year", 365, DAYS);

		private TimeUnits(String friendlyName, String friendlyNameSingular, long commonDivider,
				TimeUnits parent) {
			this(friendlyName, friendlyNameSingular, commonDivider * parent.getCommonDivider());
		}

		private TimeUnits(String friendlyName, String friendlyNameSingular, long commonDivider) {
			this.friendlyName = friendlyName;
			this.friendlyNameSingular = friendlyNameSingular;
			this.commonDivider = commonDivider;
		}

		private TimeUnits(String friendlyName, String friendlyNameSingular) {
			this(friendlyName, friendlyNameSingular, 1);
		}

		private final String friendlyName;
		private final String friendlyNameSingular;
		private final long commonDivider;

		public long getCommonDivider() {
			return commonDivider;
		}

		public String getFriendlyName() {
			return friendlyName;
		}

		public String getFriendlyNameSingular() {
			return friendlyNameSingular;
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
			builder.append(tempTime).append(' ');
			if (tempTime == 1) {
				builder.append(lowestUnit.getFriendlyNameSingular());
			} else {
				builder.append(lowestUnit.getFriendlyName());
			}
		}
		return builder.toString();
	}

}
