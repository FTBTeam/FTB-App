# Please note, although this file helps out with the build process, it is not pure and will modify files that should 
# not be committed. Please be careful when using this file.

# Variables
BUILD_DIR = $(PWD)/build
PUBLIC_DIR = $(PWD)/public
SUBPROCESS_DIR = $(PWD)/subprocess
OVERWOLF_DIR = $(PWD)/overwolf

.PHONY: create_windows_release clean_up build_subprocess build_overwolf ensure_licenses

# Create a release for windows
create_windows_release: create_build_dest build_overwolf clean_up

# Create a release for unix
create_unix_release: # Yeah, nope.  

# Create the build directory
create_build_dest:
	rm -rf $(BUILD_DIR) || true && \
	mkdir -p $(BUILD_DIR)

# Ensure the licenses are generated
ensure_licenses:
	# Check if the ./licences.json exists
	if [ ! -f ./licenses.json ]; then \
		npm run install --frozen-lockfile; \
		npm run run gen:licenses; \
	fi

# Build the shim and copy it to the build directory
build_shim:
	pushd $(OVERWOLF_DIR)/OverwolfShim && \
	dotnet build -c Release && \
	cp ./bin/Release/net48/OverwolfShim.dll $(BUILD_DIR)/OverwolfShim.dll && \
	cp ./bin/Release/net48/OverwolfShim.dll $(OVERWOLF_DIR)/OverwolfShim.dll && \
	popd

# Build the subprocess and copy it to the build directory
build_subprocess:
	pushd $(SUBPROCESS_DIR) && \
	export BRANCH_OVERRIDE=release && \
	./gradlew clean build && \
	popd

# Build the overwolf app
build_overwolf: build_shim build_subprocess ensure_licenses
	cp $(SUBPROCESS_DIR)/build/libs/*-all.jar $(OVERWOLF_DIR)/ && \
	cp $(SUBPROCESS_DIR)/build/libs/*-all.jar $(BUILD_DIR)/ && \
	cp $(SUBPROCESS_DIR)/build/libs/meta.json $(OVERWOLF_DIR)/ && \
	cp $(SUBPROCESS_DIR)/build/libs/meta.json $(BUILD_DIR)/ && \
	cp $(SUBPROCESS_DIR)/build/libs/java-licenses.json $(OVERWOLF_DIR)/ && \
	cp $(SUBPROCESS_DIR)/build/libs/java-licenses.json $(BUILD_DIR)/ && \
	npm run install --frozen-lockfile && \
	node $(PWD)/tooling/overwolf/patchManifest.js && \
	npm run run vue:build:overwolf && \
	node $(PWD)/tooling/overwolf/packageOpk.js --output $(BUILD_DIR) --zip

# Clean up after ourselves	
clean_up:
	rm -f $(OVERWOLF_DIR)/meta.json || true
	rm -f $(OVERWOLF_DIR)/java-licenses.json || true
	rm -f $(SUBPROCESS_DIR)/build/libs/*-all.jar || true
	rm -f $(OVERWOLF_DIR)/*-all.jar || true
	