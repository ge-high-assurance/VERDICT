
BIN_NAME=aadl2iml
BIN_PATH=bin

OCB_FLAGS   = -I src
OCB = ocamlbuild $(OCB_FLAGS)

.PHONY: all clean native

all: native

clean:
	$(OCB) -clean
	@rm -rf $(BIN_PATH)

native:
	$(OCB) main.native
	@mkdir -p $(BIN_PATH)
	@cp -L main.native $(BIN_PATH)/$(BIN_NAME)
