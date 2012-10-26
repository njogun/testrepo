#!/bin/perl

# tab-delimited file parser/printer
# sample input file:
#ID	DATE	NAME	CITY	VALUE
#291793292	20121005	JOHNSMITH	NEWYORK	9032641
#29180049	20121005	JOHNDOE	LOSANGELES	9105069
#30637262	20121016	JANEDOE	CHICAGO	9554666
#31024133	20121018	JOHNJOHNSON	DENVER	10355118

my $line = <>;
chomp $line;
my @header = split(/\t/, $line);
my @rows =();
while ($line = <>) {
	chomp $line;
	my @row = split(/\t/, $line);
	my %vals;
	for ($i = 0; $i < @row; $i++) {
		$vals{$header[$i]} = $row[$i];
	}
	push @rows,\%vals;
}

foreach my $row (@rows) {
	my %row = %$row;
	print "$row{NAME} $row{CITY}: $row{VALUE} ($row{ID})\n";
}

