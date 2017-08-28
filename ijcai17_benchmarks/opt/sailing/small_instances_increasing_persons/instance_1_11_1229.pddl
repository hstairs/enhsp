;;Setting seed to 1229
;; Enrico Scala (enricos83@gmail.com) and Miquel Ramirez (miquel.ramirez@gmail.com)
(define (problem instance_1_11_1229)

	(:domain sailing)

	(:objects
		b0  - boat
		p0 p1 p2 p3 p4 p5 p6 p7 p8 p9 p10  - person
	)

  (:init
		(= (x b0) 2)
(= (y b0) 0)


		(= (d p0) 32)
(= (d p1) 110)
(= (d p2) 140)
(= (d p3) 26)
(= (d p4) 64)
(= (d p5) 85)
(= (d p6) 40)
(= (d p7) 47)
(= (d p8) 89)
(= (d p9) 24)
(= (d p10) 90)


	)

	(:goal
		(and
			(saved p0)
(saved p1)
(saved p2)
(saved p3)
(saved p4)
(saved p5)
(saved p6)
(saved p7)
(saved p8)
(saved p9)
(saved p10)

		)
	)
)


