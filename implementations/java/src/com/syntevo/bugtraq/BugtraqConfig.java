/*
 * Copyright (c) 2013 by syntevo GmbH. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of syntevo GmbH nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.syntevo.bugtraq;

import java.util.*;

import org.jetbrains.annotations.*;

public final class BugtraqConfig {

	// Constants ==============================================================

	private static final String BUGTRAQ = "bugtraq";

	private static final String URL = "url";
	private static final String ENABLED = "enabled";
	private static final String LOG_REGEX = "logregex";
	private static final String LOG_FILTERREGEX = "logfilterregex";
	private static final String LOG_LINKREGEX = "loglinkregex";
	private static final String LOG_LINKTEXT = "loglinktext";
	private static final String PROJECTS = "projects";

	// Static =================================================================

	@Nullable
	public static BugtraqConfig read(@NotNull Config repoConfig, @Nullable Config baseConfig) throws BugtraqException {
		final Set<String> allNames = new HashSet<>();
		if (getString(null, URL, repoConfig, baseConfig) != null) {
			allNames.add(null);
		}
		else {
			allNames.addAll(repoConfig.getSubsections(BUGTRAQ));
			if (baseConfig != null) {
				allNames.addAll(baseConfig.getSubsections(BUGTRAQ));
			}
		}

		final List<BugtraqConfigEntry> entries = new ArrayList<>();
		for (String name : allNames) {
			final String url = getString(name, URL, repoConfig, baseConfig);
			if (url == null) {
				continue;
			}

			final String enabled = getString(name, ENABLED, repoConfig, baseConfig);
			if (enabled != null && !"true".equals(enabled)) {
				continue;
			}

			String idRegex = getString(name, LOG_REGEX, repoConfig, baseConfig);
			if (idRegex == null) {
				return null;
			}

			String filterRegex = getString(name, LOG_FILTERREGEX, repoConfig, baseConfig);
			final String linkRegex = getString(name, LOG_LINKREGEX, repoConfig, baseConfig);
			if (filterRegex == null && linkRegex == null) {
				final String[] split = idRegex.split("\n", Integer.MAX_VALUE);
				if (split.length == 2) {
					// Compatibility with TortoiseGit
					filterRegex = split[0];
					idRegex = split[1];
				}
				else {
					// Backwards compatibility with specification version < 0.3
					final List<String> logIdRegexs = new ArrayList<>();
					for (int index = 1; index < Integer.MAX_VALUE; index++) {
						final String logIdRegexN = getString(name, LOG_REGEX + index, repoConfig, baseConfig);
						if (logIdRegexN == null) {
							break;
						}

						logIdRegexs.add(logIdRegexN);
					}

					if (logIdRegexs.size() > 1) {
						throw new ConfigException("More than three " + LOG_REGEX + " entries found. This is not supported anymore since bugtraq version 0.3, use " + LOG_FILTERREGEX + " and " + LOG_LINKREGEX + " instead.");
					}
					else if (logIdRegexs.size() == 1) {
						filterRegex = idRegex;
						idRegex = logIdRegexs.get(0);
					}
				}
			}

			final String projectsList = getString(name, PROJECTS, repoConfig, baseConfig);
			final List<String> projects;
			if (projectsList != null) {
				projects = new ArrayList<>();

				final StringTokenizer tokenizer = new StringTokenizer(projectsList, ",", false);
				while (tokenizer.hasMoreTokens()) {
					projects.add(tokenizer.nextToken().trim());
				}

				if (projects.isEmpty()) {
					throw new ConfigException("'" + name + ".projects' must specify at least one project or be not present at all.");
				}
			}
			else {
				projects = null;
			}

			final String linkText = getString(name, LOG_LINKTEXT, repoConfig, baseConfig);
			entries.add(new BugtraqConfigEntry(url, idRegex, linkRegex, filterRegex, linkText, projects));
		}

		if (entries.isEmpty()) {
			return null;
		}

		return new BugtraqConfig(entries);
	}

	// Fields =================================================================

	@NotNull
	private final List<BugtraqConfigEntry> entries;

	// Setup ==================================================================

	BugtraqConfig(@NotNull List<BugtraqConfigEntry> entries) {
		this.entries = entries;
	}

	// Accessing ==============================================================

	@NotNull
	public List<BugtraqConfigEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	// Utils ==================================================================

	@Nullable
	private static String getString(@Nullable String subsection, @NotNull String key, @NotNull Config config, @Nullable Config baseConfig) throws ConfigException {
		final String value = config.getString(BUGTRAQ, subsection, key);
		if (value != null) {
			return trimMaybeNull(value);
		}

		if (baseConfig != null) {
			return trimMaybeNull(baseConfig.getString(BUGTRAQ, subsection, key));
		}

		return value;
	}

	@Nullable
	private static String trimMaybeNull(@Nullable String string) {
		if (string == null) {
			return null;
		}

		string = string.trim();
		if (string.length() == 0) {
			return null;
		}

		return string;
	}

	// Inner Classes ==========================================================

	public interface Config {
		Collection<String> getSubsections(@NotNull String section) throws ConfigException;

		String getString(@NotNull String section, @Nullable String subsection, @NotNull String key) throws ConfigException;
	}

	public static class ConfigException extends BugtraqException {
		public ConfigException(String message) {
			super(message);
		}

		public ConfigException(Throwable cause) {
			super(cause);
		}
	}
}