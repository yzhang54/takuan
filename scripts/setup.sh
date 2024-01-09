if [ "$1" = "" ] || [ "$2" = "" ]; then
    echo "Usage: ./setup.sh <gitURL> <sha> <module> <iDFlakiesLocalPath>"
    exit 1;
fi

gitURL="$1"
gitRepoName="$(basename "$gitURL" .git)"
sha="$2"
module="$3"
iDFlakiesLocalPath="$4"

cwd="$(pwd)"
#Automatically setting up the mvn project's pom.xml for iFixFlakies
cd $iDFlakiesLocalPath
bash pom-modify/modify-project.sh $cwd

cd $cwd
mvn clean install compile -Dmaven.test.skip=true

