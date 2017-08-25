;;Setting seed to 1229
(define (problem instance_2_2_1229)

	(:domain sailing)

	(:objects
		b0 b1  - boat
		p0 p1  - person
	)

  (:init
		(= (x b0) 7)
(= (y b0) 0)
(= (x b1) -7)
(= (y b1) 0)


		(= (d p0) 32)
(= (d p1) 110)


	)

	(:goal
		(and
			(saved p0)
(saved p1)

		)
	)
)


