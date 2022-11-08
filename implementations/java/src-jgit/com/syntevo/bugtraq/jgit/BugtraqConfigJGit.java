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
package com.syntevo.bugtraq.jgit;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.treewalk.*;
import org.eclipse.jgit.treewalk.filter.*;
import org.jetbrains.annotations.*;

import com.syntevo.bugtraq.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class BugtraqConfigJGit {

	// Constants ==============================================================

	private static final String DOT_GIT_BUGTRAQ = ".gitbugtraq";
	private static final String DOT_TGITCONFIG = ".tgitconfig";

	// Static =================================================================

	@Nullable
	public static BugtraqConfig read(@NotNull Repository repository) throws IOException, ConfigInvalidException {
		Config baseConfig = getBaseConfig(repository, DOT_GIT_BUGTRAQ);
		if (baseConfig == null) {
			baseConfig = getBaseConfig(repository, DOT_TGITCONFIG);
		}

		final Config config;
		try {
			config = repository.getConfig();
		}
		catch (RuntimeException ex) {
			final Throwable cause = ex.getCause();
			if (cause instanceof IOException) {
				throw (IOException)cause;
			}
			throw ex;
		}

		return BugtraqConfig.read(new ConfigWrapper(config), baseConfig != null ? new ConfigWrapper(baseConfig) : null);
	}

	// Utils ==================================================================

	@Nullable
	private static Config getBaseConfig(@NotNull Repository repository, @NotNull String configFileName) throws IOException, ConfigInvalidException {
		final Config baseConfig;
		if (repository.isBare()) {
			// read bugtraq config directly from the repository
			String content = null;
			RevWalk rw = new RevWalk(repository);
			TreeWalk tw = new TreeWalk(repository);
			tw.setFilter(PathFilterGroup.createFromStrings(configFileName));
			try {
				final Ref ref = repository.findRef(Constants.HEAD);
				if (ref == null) {
					return null;
				}

				ObjectId headId = ref.getTarget().getObjectId();
				if (headId == null || ObjectId.zeroId().equals(headId)) {
					return null;
				}

				RevCommit commit = rw.parseCommit(headId);
				RevTree tree = commit.getTree();
				tw.reset(tree);
				while (tw.next()) {
					ObjectId entid = tw.getObjectId(0);
					FileMode entmode = tw.getFileMode(0);
					if (FileMode.REGULAR_FILE == entmode) {
						ObjectLoader ldr = repository.open(entid, Constants.OBJ_BLOB);
						content = new String(ldr.getCachedBytes(), guessEncoding(commit));
						break;
					}
				}
			}
			finally {
				rw.dispose();
				tw.close();
			}

			if (content == null) {
				// config not found
				baseConfig = null;
			}
			else {
				// parse the config
				Config config = new Config();
				config.fromText(content);
				baseConfig = config;
			}
		}
		else {
			// read bugtraq config from work tree
			final File baseFile = new File(repository.getWorkTree(), configFileName);
			if (baseFile.isFile()) {
				FileBasedConfig fileConfig = new FileBasedConfig(baseFile, repository.getFS());
				fileConfig.load();
				baseConfig = fileConfig;
			}
			else {
				baseConfig = null;
			}
		}
		return baseConfig;
	}

	@NotNull
	private static Charset guessEncoding(RevCommit commit) {
		try {
			return commit.getEncoding();
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			return UTF_8;
		}
	}

	// Inner Classes ==========================================================

	private static class ConfigWrapper implements BugtraqConfig.Config {
		private final Config config;

		public ConfigWrapper(Config config) {
			this.config = config;
		}

		@Override
		public Collection<String> getSubsections(@NotNull String section) throws BugtraqConfig.ConfigException {
			try {
				return config.getSubsections(section);
			}
			catch (RuntimeException ex) {
				throw new BugtraqConfig.ConfigException(ex);
			}
		}

		@Override
		public String getString(@NotNull String section, @Nullable String subsection, @NotNull String key) throws BugtraqConfig.ConfigException {
			try {
				return config.getString(section, subsection, key);
			}
			catch (Exception ex) {
				throw new BugtraqConfig.ConfigException(ex);
			}
		}
	}
}