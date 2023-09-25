SCRIPTS_DIR=$(dirname "$0")
cd $SCRIPTS_DIR/../
mvn install
mvn dependency:copy-dependencies