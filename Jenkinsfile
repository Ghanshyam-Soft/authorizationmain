import java.util.regex.Matcher

if (params.cicdLibraryBranch){
    library "cicd-shared-lib@${params.cicdLibraryBranch}"
} else {
    library "cicd-shared-lib"
}

String jenkEnvDump = "--- Start Jenkins Environment ---\n"
jenkEnvMap = env.getEnvironment()
    
for ( envKey in jenkEnvMap.sort() ) {
    jenkEnvDump += "${envKey}\n"
}

jenkEnvMap = null
jenkEnvDump += "--- End Jenkins Environment ---\n"
echo jenkEnvDump

String sysEnvDump = "--- Start System Environment ---\n"
sysEnvMap = System.getenv()
    
for ( envKey in sysEnvMap.sort() ) {
    sysEnvDump += "${envKey}: ${sysEnvMap.envKey}\n"
}
    
sysEnvMap = null
sysEnvDump += "--- End System Environment ---\n"

echo sysEnvDump

List projectRepoBranch = determineProjectRepoBranchFromScm(scm)

String projectName = projectRepoBranch[0]
String repoName = projectRepoBranch[1]
String branchName = projectRepoBranch[2]

dynamicPipeline 'projectName': projectName , 'repoName': repoName, 'branchName': branchName

List determineProjectRepoBranchFromScm(scmObj) {
    // Figure out our repo URL, projectName, repoName, and branchName from the scm obj we're given
    def repoObj = scmObj.getRepositories().get(0)
    def repoRefSpec = repoObj.getFetchRefSpecs().get(0).toString()

    // URL is special, we'll parse it to derive our project/repo values
    def repoURL = repoObj.getURIs().get(0)
    String repoURLString = repoURL.toString()

    println "repoURL: ${repoURLString}, repoRefSpec: ${repoRefSpec}"

    return determineProjectRepoBranchFromURL(repoURL)
}

List determineProjectRepoBranchFromURL(repoURL) {

    List AUTHORIZED_URLS = [
    	[host: "bitbucket.staples.com", port: 7999, ]
    ]

    String urlPath = repoURL.getPath()
    String urlHost = repoURL.getHost()
    int urlPort = repoURL.getPort()

    String projectName = ""
    String repoName = ""

    // If we recognize one of our Stash URLs then we know the structure to project/repo
    for ( url in AUTHORIZED_URLS ) {
        if ( urlHost == url['host'] && urlPort == url['port'] ) {
            m = urlPath =~ /(?:(?<projectName>[^\/]+)\/)?(?<repoName>[^\/]+)\.git$/
            assert m instanceof Matcher
            if (m) {
                projectName = m.group('projectName')
                repoName = m.group('repoName')
            }

            // clear out non-serializable objects!!!
            m = null
            break
        }
    }

    if ( projectName == "" || repoName == "") {
        throw new Exception("Could not identify project/repo name in URL "+
        repoURL.toString() + " or hostname does not match hosts in " +
        AUTHORIZED_URLS)
    }

    String branchName = getPipelineBranch()

    return [
        projectName,
        repoName,
        branchName
    ]
}

String getPipelineBranch() {
    String branchName = params.PipelineBranch

    if (branchName == null) {
        branchName = env.BRANCH_NAME
    }

    if ( branchName == null ) {
        println "using default branchName master"
        branchName = "master"
    }

    return branchName
}