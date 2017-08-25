
(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket a b -tank)
    (:init
    
        ;;a information
        (= (volume a) 100)
        (= (capacity a) 150)
        (= (flow-constant a) 0.8)

        ;;b information
        (= (volume b) 100)
        (= (capacity b) 150)
        (= (flow-constant b) 0.8)


        ;;bucket information
        (= (volume bucket) 0)
        (= (capacity bucket) 150)
        
        ;;topology
	    (connected a bucket)
   	    (connected b bucket)
    )
    (:goal 
	    (and
            (>= (volume bucket) 75)
	    )
    )
)
