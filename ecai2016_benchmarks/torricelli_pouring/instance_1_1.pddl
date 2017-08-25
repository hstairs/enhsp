
(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket a -tank)
    (:init
        (= (volume a) 100)
        (= (volume bucket) 0)
        (= (flow-constant a) 0.8)
        (= (capacity a) 150)
        (= (capacity bucket) 150)
	    (connected a bucket)
    )
    (:goal 
	    (and
            (>= (volume bucket) 75)
	    )
    )
)
