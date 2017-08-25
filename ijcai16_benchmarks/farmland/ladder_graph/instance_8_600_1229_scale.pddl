;; Enrico Scala (enricos83@gmail.com) and Miquel Ramirez (miquel.ramirez@gmail.com)
;;Setting seed to 1229
(define (problem instance_8_600_1229_scale)
	(:domain farmland)
	(:objects
		farm0 farm1 farm2 farm3 farm4 farm5 farm6 farm7  - farm
	)
  (:init
		(= (x farm0) 1)
		(= (x farm1) 0)
		(= (x farm2) 600)
		(= (x farm3) 0)
		(= (x farm4) 0)
		(= (x farm5) 0)
		(= (x farm6) 1)
		(= (x farm7) 0)
		
		(adj farm0 farm1)
		(adj farm0 farm4)
		(adj farm1 farm0)
		(adj farm1 farm2)
		(adj farm1 farm5)
		(adj farm2 farm1)
		(adj farm2 farm3)
		(adj farm2 farm6)
		(adj farm3 farm2)
		(adj farm3 farm7)
		(adj farm4 farm0)
		(adj farm4 farm5)
		(adj farm5 farm1)
		(adj farm5 farm4)
		(adj farm5 farm6)
		(adj farm6 farm2)
		(adj farm6 farm5)
		(adj farm6 farm7)
		(adj farm7 farm3)
		(adj farm7 farm6)
		
		(= (cost) 0)
	)
	(:goal
		(and
			(> (x farm0) 0)
			(> (x farm1) 0)
			(> (x farm2) 0)
			(> (x farm3) 0)
			(> (x farm4) 0)
			(> (x farm5) 0)
			(> (x farm6) 0)
			(> (x farm7) 0)
			
			(> (+ (* 1.5 (x farm0))(+ (* 1.3 (x farm1))(+ (* 1.0 (x farm2))(+ (* 1.2 (x farm3))(+ (* 1.4 (x farm4))(+ (* 1.4 (x farm5))(+ (* 2.0 (x farm6))(+ (* 1.4 (x farm7)) 0)))))))) 840.0)
		)
	)
)


