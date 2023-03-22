# UnrolledLinkedList
Implementation of the UnrolledLinkedList data structure with no external dependencies.

In order to use this, you can simply copy the `UnrolledLinkedList.java` to your code 
and adjust the package.\
_In the future, I may add a maven artifact that can be added in a more official capacity._

Althought I try to do reasonable testing and performance checking, *I can not assure 
absolute reliability* as I have not done a deep dive in to List interface testing. I have 
used this implementation in personal use on non trivial use cases and have not recorded 
any bugs. 

The performance is probably not the absolute best but it has performed significantly better
on random insertions (Remain sorted insert use case) than array list.\
Please do your own performance tests. Always. Computers are weird

Thanks for reading! 😃
