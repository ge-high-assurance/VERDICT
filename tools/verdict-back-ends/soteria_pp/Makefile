LOCAL_BINDIR=$(CURDIR)/bin

build:
	@dune build @install
### Create 'bin' directory and copy soteria_pp binary there (discard messages)
	@dune install --sections=bin --prefix . 2> /dev/null

clean:
	@dune clean
	@rm -rf $(LOCAL_BINDIR)
