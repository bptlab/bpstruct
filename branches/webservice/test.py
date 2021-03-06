import urllib2, urllib

#url = "http://141.89.225.233/bpstruct/rest/v1/check/structure"
url = "http://141.89.225.233/bpstruct/rest/v1/structure/max"
#url = "http://localhost:8080/bpstruct/rest/v1/structure/max"
data = """{'process' : {'name' : 'test case',
			'tasks' : [{'id' : 'task1', 'label' : 'Task 1'},
				{'id' : 'task2', 'label' : 'Task 2'},
				{'id' : 'task3', 'label' : 'Task 3'},
				{'id' : 'task4', 'label' : 'Task 4'}], 
			'gateways' : [{'id' : 'gate1', type : 'XOR'},
				{'id' : 'gate2', type : 'XOR'}],
			'flows' : [{'src' : 'task1', 'tgt' : 'gate1', 'label' : null},
				{'src' : 'gate1', 'tgt' : 'task2', 'label' : 'x > 3'},
				{'src' : 'gate1', 'tgt' : 'task3', 'label' : 'x <= 3'},
				
				{'src' : 'task3', 'tgt' : 'gate2', 'label' : null}
				]},
		'options': {'json': true, 'dot': true}}"""
#{'src' : 'task2', 'tgt' : 'gate2', 'label' : null},{'src' : 'gate2', 'tgt' : 'task4', 'label' : null},
acyclic = """{'name' : 'test case',
			'tasks' : [{'id' : 'a', 'label' : 'A'},
				{'id' : 'b', 'label' : 'B'},
				{'id' : 'c', 'label' : 'C'},
				{'id' : 'd', 'label' : 'D'},
				{'id' : 'e', 'label' : 'E'},
				{'id' : 'f', 'label' : 'F'},
				{'id' : 'i', 'label' : 'I'},
				{'id' : 'o', 'label' : 'O'}],
			'gateways' : [{'id' : 't', type : 'XOR'},
				{'id' : 'u', type : 'AND'},
				{'id' : 'v', type : 'AND'},
				{'id' : 'w', type : 'XOR'},
				{'id' : 'x', type : 'XOR'},
				{'id' : 'y', type : 'AND'},
				{'id' : 'z', type : 'XOR'}],
			'flows' : [{'src' : 'i', 'tgt' : 't', 'label' : null},
				{'src' : 't', 'tgt' : 'a', 'label' : null},
				{'src' : 't', 'tgt' : 'b', 'label' : null},
				{'src' : 't', 'tgt' : 'e', 'label' : null},
				{'src' : 'a', 'tgt' : 'u', 'label' : null},
				{'src' : 'b', 'tgt' : 'v', 'label' : null},
				{'src' : 'u', 'tgt' : 'w', 'label' : null},
				{'src' : 'u', 'tgt' : 'x', 'label' : null},
				{'src' : 'v', 'tgt' : 'w', 'label' : null},
				{'src' : 'v', 'tgt' : 'x', 'label' : null},
				{'src' : 'w', 'tgt' : 'c', 'label' : null},
				{'src' : 'x', 'tgt' : 'd', 'label' : null},
				{'src' : 'c', 'tgt' : 'y', 'label' : null},
				{'src' : 'd', 'tgt' : 'y', 'label' : null},
				{'src' : 'e', 'tgt' : 'f', 'label' : null},
				{'src' : 'y', 'tgt' : 'z', 'label' : null},
				{'src' : 'f', 'tgt' : 'z', 'label' : null},
				{'src' : 'z', 'tgt' : 'o', 'label' : null}]}"""

cyclic = """{'process' : {'name' : 'test case',
			 'tasks' : [{'id' : 'a', 'label' : 'A'},
			 	{'id' : 'b', 'label' : 'B'},
			 	{'id' : 'c', 'label' : 'C'},
			 	{'id' : 'd', 'label' : 'D'},
			 	{'id' : 'i', 'label' : 'I'},
			 	{'id' : 'o', 'label' : 'O'}],
			 'gateways' : [{'id' : 'u', type : 'AND'},
			 	{'id' : 'v', type : 'XOR', 'label' : 'v'},
			 	{'id' : 'w', type : 'XOR', 'label' : 'w'},
			 	{'id' : 'x', type : 'AND', 'label' : 'x'},
			 	{'id' : 'y', type : 'XOR'},
			 	{'id' : 'z', type : 'AND'}],
			'flows' : [{'src' : 'i', 'tgt' : 'u', 'label' : null},
				{'src' : 'u', 'tgt' : 'a', 'label' : null},
				{'src' : 'u', 'tgt' : 'w', 'label' : null},
				{'src' : 'a', 'tgt' : 'v', 'label' : null},
				{'src' : 'w', 'tgt' : 'c', 'label' : null},
				{'src' : 'v', 'tgt' : 'b', 'label' : null},
				{'src' : 'b', 'tgt' : 'x', 'label' : null},
				{'src' : 'c', 'tgt' : 'x', 'label' : null},
				{'src' : 'x', 'tgt' : 'd', 'label' : null},
				{'src' : 'd', 'tgt' : 'y', 'label' : null},
				{'src' : 'y', 'tgt' : 'z', 'label' : null},
				{'src' : 'z', 'tgt' : 'v', 'label' : null},
				{'src' : 'z', 'tgt' : 'w', 'label' : null},
				{'src' : 'y', 'tgt' : 'o', 'label' : null}]},
			'options' : {'json' : true, 'dot' : true}}"""
result = urllib2.urlopen(url, data)
print result.info()
print result.read()