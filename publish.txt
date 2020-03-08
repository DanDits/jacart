
# Signing:
gpg --version # check if gpg is installed and what version
gpg --gen-key # generate a new key, follow prompts
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys 5A4178EE814AABC4 # published this key, expires 2022-03-07

# Deploying/ publishing with maven:
# edit .m2/settings.xml to include setterings.servers.server with id=ossrh,username=DanDits,password=... Use this id(ossrh) in the pom.xml's distributionManagement.(snapshot)Repository.id
mvn clean deploy # deploys the artifact. Requires the maven-deploy-plugin. If version ends with -SNAPSHOT it will be deployed as such
# for non snapshot deployments, log in into https://oss.sonatype.org with DanDits and PW
# Note: This will not yet sign anything, it will not provide sources or javadoc. When attempting to close this repository in the Nexus it will show these steps as failed in the Activity panel.
# We can drop repositories that failed in Nexus, optionally providing a message that will be sent to those registered to listen to that
# Alternative (recommended: nexus-staging-maven-plugin. Use it with autoReleaseAfterClose=false to inspect manually. Then deploy with "mvn nexus-staging:release" or drop with "mvn nexus-staging:drop"

# How to get javadoc, sources and signing in: (see https://central.sonatype.org/pages/apache-maven.htlm#javadoc-and-sources-attachements
# requires: maven-source-plugin with execution:attach-sources (goal:jar-no-fork)
# requires: maven-javadoc-plugin with execution:attach-javadocs (goal:jar)
# requires: maven_gpg-plugin with execution:sign-artifacts(phase:verify,goals:sign)

# when everything works, the staging repository closing works, it sends an email to me and there is now a Release button

# to update to a new version one can use mvn versions:set -DnewVersion=1.2.3
mvn clean deploy -P release # deploy and use release profile to include plugins for javadoc, source and signing

#Note: credentials to pgp key must not be in the settings.xml, my system at least will prompt me.
#Note: