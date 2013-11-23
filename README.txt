Git Bugtraq Configuration specification
=======================================
Version 0.2.3, 2013-11-23


1. Introduction
---------------

Git commits are often related to specific issues (or bugs). The Git
Bugtraq Configuration allows to associate a Git commit with a
corresponding issue ID. Git clients may use this association to provide
additional functionality, like displaying a hyperlink for issue IDs
within a commit message which links to the appropriate issue tracker
web page.

The Git Bugtraq Configuration is similar to Subversion bugtraq
properties[1] and includes concepts of Gerrit commentlinks[2].


2. Configuration options
------------------------

The main configuration namespace is 'bugtraq'. <name> is the
configuration name (see section 3).

* bugtraq.<name>.url (mandatory)

specifies the URL of the bug tracking system. It must be properly URI
encoded and it has to contain %BUGID%. %BUGID% will be replaced by the
Git client with a concrete issue ID.


* bugtraq.<name>.logRegex (mandatory) and
  bugtraq.<name>.logRegex<N> (optional)

specify Perl Compatible Regular Expressions[3] which will be used to
extract the issue ID from a commit message.

logRegex must contain exactly one matching group, which represents the
extracted BUGID. There may be additional non-matching groups ('(?:').
logRegex matches case-sensitive unless explicitly set to case-
insensitive ('(?i)').

If there are optional logRegex1, logRegex2, ... present, the extracted
results of logRegex is used as input for logRegex1, the result of
logRegex1 is used as input of logRegex2 and so on. logRegex1 ...
logRegexN must be consecutively numbered and will be applied in
numbering order. If any of logRegex ... logRegexN do not match, no
BUGID will be extracted.

Example: with logRegex set to

  [Ii]ssues?:?(\s*(,|and)?\s*#\d+)+

and logRegex1 set to

  (\d+)

and having a commit message like

  Issues #3, #4 and #5: Git Bugtraq Configuration options (see #12)

logRegex will pick "Issues #3, #4 and #5" and logRegex1 will pick "3", "
4" and "5".

Note: in Git-Config-like files, backslashes needs to be escaped (see
section 5).

* bugtraq.<name>.enabled (optional)

specifies whether this Bugtraq Configuration is enabled. It defaults to
'true'.


3. Multiple configurations
--------------------------

There can be multiple configurations, either to support multiple issue
trackers or alternative configurations for the same issue tracker. In
the latter case, probably only one of these configurations will be
enabled.

Every configuration uses its own namespace 'bugtraq.<name>'. For a
single commit message, all configurations will be applied, possibly
giving multiple issue links to different issue trackers for a single
commit message.

3.1 Handling of intersecting issues IDs

If two issue IDs are intersecting (due to intersecting configurations),
only the issue ID with the lower starting position will be displayed.
The other issue ID will be ignored.


4. Configuration files
----------------------

There are two places where the configuration options can be specified:

* in the .gitbugtraq file in the repository root. This file is using
  the default Git config file layout.

* in $GIT_DIR/config

Options specified in $GIT_DIR/config will override options from
.gitbugtraq.

An example content of .gitbugtraq (note, that '\' need to be escaped):

  [bugtraq "tracker"]
    url = https://host/browse/SG-%BUGID%
    logRegex = \\d+
    logRegex1 = SG-(\\d+)
    
Exactly the same lines could be added as an additional section to
$GIT_DIR/config as well.


5. logRegEx examples
--------------------

* From messages like "Fix: #1" or "fixes:  #1, #2 and #3",
  the "1", "2" and "3" should be extracted.

  logRegex =  "(?i)fix(?:es)?\\: ((\\s*(,|and)?\\s*#\\d+)+)"
  logRegex1 = (\\d+) 

* From messages like "Bug: #1" or "Bug IDs: #1; #2; #3" or
  "Cases: #1, #2" the "1", "2" and "3" should be extracted.

  logRegex = "(?i)(?:Bug[zs]?\\s*IDs?\\s*|Cases?)[#:; ]+((\\d+[ ,:;#]*)+)"
  logRegex1 = \\d+
  

References
----------

[1] http://tortoisesvn.net/docs/release/TortoiseSVN_en/
    tsvn-dug-bugtracker.html

[2] https://gerrit-review.googlesource.com/Documentation/
    config-gerrit.html#_a_id_commentlink_a_section_commentlink

[3] http://perldoc.perl.org/perlre.html

