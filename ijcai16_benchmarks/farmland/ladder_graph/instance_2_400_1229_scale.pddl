;; Enrico Scala (enricos83@gmail.com) and Miquel Ramirez (miquel.ramirez@gmail.com)
;;Setting seed to 1229
(define (problem instance_2_400_1229_scale)
	(:domain farmland)
	(:objects
		farm0 farm1  - farm
	)
  (:init
		(= (x farm0) 400)
		(= (x farm1) 1)
		
		(adj farm0 farm1)
		(adj farm1 farm0)
		
		(= (cost) 0)
	)
	(:goal
		(and
			(> (x farm0) 0)
			(> (x farm1) 0)
			
			(> (+ (* 1.0 (x farm0))(+ (* 1.5 (x farm1)) 0)) 560.0)
		)
	)
)


