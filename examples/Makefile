BCOMP=b-comp
BAS=b-as
BLINK=b-link

PRGS=lower e

all: ${PRGS}

lower: lower.bo
	${BLINK} $< -o $@

e: e.bo
	${BLINK} $< -o $@

%.bo: %.bs
	${BAS} $< -o $@

%.bs: %.b
	${BCOMP} $< -o $@

clean:
	rm -f *.bo *.bs ${PRGS}
