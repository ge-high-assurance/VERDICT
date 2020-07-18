
BIN_NAME=aadl2iml
BIN_PATH=bin
EXE_PATH=_build/default/src/main.exe

.PHONY: all clean build

all: build

clean:
	@dune clean
	@rm -rf $(BIN_PATH)

build:
	@dune build
	@mkdir -p $(BIN_PATH)
	@cp $(EXE_PATH) $(BIN_PATH)/$(BIN_NAME)
