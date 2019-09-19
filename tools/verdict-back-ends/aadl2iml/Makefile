
BIN_NAME=aadl2iml
BIN_PATH=bin/$(BIN_NAME)

OCB_FLAGS   = -I src
OCB = ocamlbuild $(OCB_FLAGS)

.PHONY: all clean native

all: native

clean:
	$(OCB) -clean
	@rm $(BIN_PATH)

native:
	$(OCB) main.native
	@mkdir -p bin
	@cp -L main.native bin/aadl2iml
