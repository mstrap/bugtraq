# Git Bugtraq Configuration specification

**Version 0.4, 2016-07-15**

## 1. Introduction

Git commits are often related to specific issues (or bugs). The Git Bugtraq Configuration allows to associate a Git commit with a corresponding issue ID. Git clients may use this association to provide additional functionality, like displaying a hyperlink for issue IDs within a commit message which links to the appropriate issue tracker web page.

The Git Bugtraq Configuration is similar to Subversion bugtraq properties[^1] and includes concepts of Gerrit commentlinks[^2].

## 2. Configuration options

The main configuration namespace is `bugtraq`.

### bugtraq.url (mandatory)

Specifies the URL of the bug tracking system. It must be properly URI encoded and it has to contain `%BUGID%`. `%BUGID%` will be replaced by the Git client with a concrete issue ID.

### bugtraq.logregex (mandatory), bugtraq.loglinkregex (optional), and bugtraq.logfilterregex (optional)

Specify Perl Compatible Regular Expressions[^3] which will be used to extract the issue ID from a commit message.

`logregex` must contain exactly one matching group, which extracts BUGIDs.

If present, `loglinkregex` will be applied before `logregex`. It must contain exactly one matching group, which extracts parts from the commit message that should show up as a link. `logregex` will then be applied to every such link to extract the actual BUGID (which is a part of the entire link). If `loglinkregex` is set, `logregex` must extract exactly one BUGID.

If present, `logfilterregex` will be applied before `logregex` (or `loglinkregex`, resp.). It must contain exactly one matching group which extracts arbitrary parts of the commit message that will be used as input for `logregex` (or `loglinkregex`, resp.).

Every of these regular expressions may contain additional non-capturing groups (`(?:)` and matches case-sensitive unless explicitly set to case-insensitive (`(?i)`). The overall extraction looks as follows:

```
commit message -> logfilterregex -> loglinkregex -> logregex -> BUGID
```

**Example:** with `logfilterregex` set to

```
[Ii]ssues?:?((\s*(,|and)?\s*#\d+)+)
```

`loglinkregex` set to

```
#\d+
```

`logregex` set to

```
\d+
```

and having a commit message like

```
Issues #3, #4 and #5: Git Bugtraq Configuration options (see rule #12)
```

`logfilterregex` will pick `Issues #3, #4 and #5`, `loglinkregex` will pick `#3`, `#4`, `#5` and `logregex` will pick `3`, `4` and `5`.

**Note:** in Git-Config-like files, backslashes need to be escaped (see section 5).

### bugtraq.loglinktext (optional)

Specifies a substitution text which will be used to display issue links extracted by `logregex` and `loglinkregex`, resp. `loglinktext` must contain `%BUGID%` which will be replaced by the concrete issue ID.

**Example:** with `logregex` set to `#?(\d+)` and `loglinktext` set to `#%BUGID%`, a commit message like

```
Issue #3, 4, 5: message
```

will be substituted to

```
Issue #3, #4, #5: message
```

with `#3`, `#4`, `#5` being links to the corresponding issues.

### bugtraq.enabled (optional)

Specifies whether this Bugtraq Configuration is enabled. It defaults to `true`.

### bugtraq.projects (optional)

Specifies a comma-separated list of JIRA _projects_ where for each project the bugtraq functionality will be applied. This works by looping over all specified projects and substituting `%PROJECT%` by the project key. Substitution of `%PROJECT%` will be performed for `bugtraq.url`, `bugtraq.logregex`, `bugtraq.loglinkregex`, `bugtraq.logfilterregex`, and `bugtraq.loglinktext`. See examples.

## 3. Multiple configurations

There can be multiple configurations, either to support multiple issue trackers or alternative configurations for the same issue tracker. In the latter case, probably only one of these configurations will be enabled.

When using multiple configurations, the `bugtraq` namespace is separated into multiple, disjoint sub-namespaces `bugtraq.<name>`, one for each configuration.

**Example:**

```
bugtraq.tracker1.url=...
bugtraq.tracker1.logregex=...
bugtraq.tracker2.url=...
bugtraq.tracker2.logregex=...
```

For a single commit message, all configurations will be applied, possibly giving multiple issue links to different issue trackers for a single commit message.

### 3.1 Handling of intersecting issues IDs

If two issue IDs are intersecting (due to intersecting configurations), only the issue ID with the lower starting position will be displayed. The other issue ID will be ignored.

## 4. Configuration files

There are two places where the configuration options can be specified:

* in the `.gitbugtraq` file in the repository root. This file is using the default Git config file layout.
* in `$GIT_DIR/config`

Options specified in `$GIT_DIR/config` will override options from `.gitbugtraq`. A `.gitbugtraq` file is processed as Git config file, i.e., the same rules for quoting and escaping apply.

An example content of `.gitbugtraq` (note, that `\` need to be escaped and `"` is used to escape the `#`):

```
[bugtraq]
  url = "https://host/#root/browse/SG-%BUGID%"
  loglinkregex = SG-\\d+
  logregex = \\d+
```

Exactly the same lines could be added as an additional section to `$GIT_DIR/config` as well.

## 5. logregex examples

From messages like `Fix: #1` or `fixes:  #1, #2 and #3`, the `1`, `2` and `3` should be extracted and the numbers including hash-sign (#) should show up as links:

```
logfilterregex =  "(?i)fix(?:es)?\\: ((\\s*(,|and)?\\s*#\\d+)+)"
loglinkregex = #\\d+
logregex = \\d+
```

From messages like `Bug: #1` or `Bug IDs: #1; #2; #3` or `Cases: #1, #2` the `1`, `2` and `3` should be extracted and only the numbers itself should show up as links:

```
logfilterregex = "(?i)(?:Bug[zs]?\\s*IDs?\\s*|Cases?)[#:; ]+((\\d+[ ,:;#]*)+)"
logregex = \\d+
```

From a message like `PRJA-1: test, PRJB-2: tset` with following configuration:

```
projects = PRJA, PRJB
url = https://server/browse/%PROJECT%-%BUGID%
loglinkregex = %PROJECT%-\\d+
logregex = \\d+
```

Issue ID `1` will be extracted and linked to:

```
https://server/browse/PRJA-1
```

and issue ID `2` will be extracted and linked to:

```
https://server/browse/PRJB-1
```

## References

[^1]: [Subversion bugtraq properties](http://tortoisesvn.net/docs/release/TortoiseSVN_en/tsvn-dug-bugtracker.html)
[^2]: [Gerrit commentlinks](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#_a_id_commentlink_a_section_commentlink)
[^3]: [Perl Compatible Regular Expressions](http://perldoc.perl.org/perlre.html)
