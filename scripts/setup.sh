if [ "$1" = "" ] || [ "$2" = "" ]; then
    echo "Usage: ./setup.sh <gitURL> <sha> (<module>)"
    exit 1;
fi

gitURL="$1"
gitRepoName="$(basename "$gitURL" .git)"
sha="$2"
module="$3" # may be empty if no module is set

echo "TODO: implement setup"
