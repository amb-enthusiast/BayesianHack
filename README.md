# Bayesian Hack

This is a small project that follows the Expectation Maximisation (EM) tutorial here:
http://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-825-techniques-in-artificial-intelligence-sma-5504-fall-2002/lecture-notes/Lecture18FinalPart1.pdf


With the following probabilistic graphical model (PGM) libraries:

* Dimple (http://dimple.probprog.org/home)
* MALLET via GRMM (http://mallet.cs.umass.edu/grmm/)
* SamIam via inflib.jar (http://reasoning.cs.ucla.edu/samiam/)
* SMILE via JSmile (http://genie.sis.pitt.edu/index.php/downloads)


I also performed hand-calculations in a spreadsheet to provide a sanity-check on the results.


### Running the project

The repo is a Java Maven project, built in Netbeans 7.3.

The project contains 4 packages, one for each library.

Each package contains a class with a main, so that they can be executed.

The Dimple, GRMM and SamIam examples should run "as is" - that is, you should just be able to run the file from the IDE (or CLI with classpath including the respective jar files).

SMILE is more challenging - the SMILE example uses JSmile, the JNI interface for the C++ SMILE library.

In order to execute the SMILE example, you will need to download SMILE binaries and the JSmile JNI files from the SMILE website:
http://genie.sis.pitt.edu/index.php/about


Next, you will need to edit your environment PATH var to include the SMILE C++ binaries.
The Maven config for the project contains the -Djava.library.path runtime argument.  It is currently set to /path/to/smile/JNI/ - you will need to change this to point to the directory on your system that includes the JNI files for JSmile.

Resources: Required trainng data (simple CSV), SAMIAM initial model file (a SamIam .net file) and the hand-calc spreadhseet are included in the src/main/resources directory.


### Observations

Dimple - Created a Dimple example of EM, following the Dimple Java API docs.  Learned probability results agree with hand calcs, GRMM and SAMIAM Results, which is cool.

GRMM - I used the factor operations in GRMM to implement EM on this model.  This is pretty neat, but a little cumbersome to get set up and difficult to generalise.

SAMIAM - I found that the iteration and thresholding didn't work (or at least I couldn't set it up correctly) so I wrote my own iteration loop.  This did require writing out .net files to disk and resetting the LearningData object in each iteration (ouch!).  I've mailed the SamIam team, but had no response as yet.

The results from Dimple, GRMM and SamIam agreed with my hand-crafted calculations - which match values described in the tutorial.

SMILE - The documentation of Smile and JSmile is a little sparse, though there is enough to get going.  However, the resultant learned CPTs did not match GRMM, SamIam or the tutorial values.... there is not enough info on the site to easily understand what is going on.  I posted to the JSmile forum and got some useful info here:
* http://genie.sis.pitt.edu/forum/viewtopic.php?f=2&t=1098



If anyone has any advice, improvements or suggestions on how best to use these libraries, I'd be grateful to hear it!
