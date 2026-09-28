package net.cyootlabs.rocamocha.platform;

/**
 * Small platform abstraction so the common code can call into loader-specific behaviour without
 * linking against any loader API. Each loader sets its implementation during mod construction.
 */
public final class Services {
	private static IPlatformHelper platform;

	private Services() {
	}

	public static void setPlatform(IPlatformHelper helper) {
		platform = helper;
	}

	public static IPlatformHelper get() {
		if (platform == null)
			throw new IllegalStateException("Platform helper has not been set by the loader entry point");
		return platform;
	}
}
