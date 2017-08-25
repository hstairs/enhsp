(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket1 bucket2 a b -tank)
    (:init
    
        ;;a information
        (= (volume a) 100)
        (= (capacity a) 150)
        (= (flow-constant a) 0.8)

        ;;b information
        (= (volume b) 100)
        (= (capacity b) 150)
        (= (flow-constant b) 0.8)


        ;;bucket1 information
        (= (volume bucket1) 0)
        (= (capacity bucket1) 150)
        
        ;;bucket2 information
        (= (volume bucket2) 0)
        (= (capacity bucket2) 150)
        
        ;;topology
	    (connected a bucket1)
   	    (connected b bucket2)
    )
    (:goal 
	    (and
            (>= (volume bucket1) 40)
            (>= (volume bucket2) 40)
	    )
    )
)
