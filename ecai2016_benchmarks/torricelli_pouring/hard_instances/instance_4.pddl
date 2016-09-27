
(define 
    (problem instance1)
    (:domain pouring)
    (:objects bucket a b c d -tank)
    (:init
    
        ;;a information
        (= (volume a) 35)
        (= (capacity a) 150)
        (= (flow-constant a) 0.8)

        ;;b information
        (= (volume b) 35)
        (= (capacity b) 150)
        (= (flow-constant b) 0.8)
        
        ;;c information
        (= (volume c) 5)
        (= (capacity c) 150)
        (= (flow-constant c) 0.8)
        
        ;;d information
        (= (volume d) 5)
        (= (capacity d) 150)
        (= (flow-constant d) 0.8)


        ;;bucket information
        (= (volume bucket) 0)
        (= (capacity bucket) 150)
        
        ;;topology
        (connected c a)
        (connected d b)
	    (connected a bucket)
   	    (connected b bucket)
    )
    (:goal 
	    (and
            (>= (volume bucket) 75)
	    )
    )
)
